/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.lastfm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Client for the Last.fm Web API 2.0 (https://www.last.fm/api).
 *
 * All calls are POSTed to `https://ws.audioscrobbler.com/2.0/` with
 * `format=json`. Authenticated calls carry a session key (`sk`) and are signed
 * with an MD5 `api_sig` — the signature is the MD5 of every `keyvalue` pair
 * sorted alphabetically by key, concatenated, with the shared secret appended.
 *
 * Initialize once from [dev.brahmkshatriya.echo.dhun.App] via [initialize]
 * using `BuildConfig.LASTFM_API_KEY` / `BuildConfig.LASTFM_SECRET`.
 */
object LastFM {

    private const val API_ROOT = "https://ws.audioscrobbler.com/2.0/"

    /** Default scrobble threshold: 50% of the track, capped at 4 minutes. */
    const val DEFAULT_SCROBBLE_DELAY_PERCENT: Float = 0.5f

    /** Tracks shorter than this (seconds) are never scrobbled. */
    const val DEFAULT_SCROBBLE_MIN_SONG_DURATION: Int = 30

    /** Last.fm counts a scrobble after half the track or 4 minutes. */
    const val DEFAULT_SCROBBLE_DELAY_SECONDS: Int = 240

    private var apiKey: String? = null
    private var secret: String? = null

    /**
     * The authenticated session key. Set at startup from the
     * `LastFMSessionKey` preference, or populated by [getSession].
     */
    var sessionKey: String? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    class LastFMException(message: String) : Exception(message)

    // ── Setup ───────────────────────────────────────────────────────────────

    /**
     * Initializes the client with the application credentials. Must be called
     * before any other [LastFM] function.
     */
    fun initialize(apiKey: String, secret: String) {
        this.apiKey = apiKey
        this.secret = secret
    }

    val isInitialized: Boolean
        get() = !apiKey.isNullOrBlank() && !secret.isNullOrBlank()

    // ── Public API (used by ScrobbleManager) ────────────────────────────────

    /**
     * Scrobbles a track (method `track.scrobble`).
     *
     * @param artist artist name.
     * @param track track title.
     * @param duration track duration in seconds.
     * @param timestamp Unix timestamp (seconds) of when the track started.
     * @param album optional album name.
     * @throws LastFMException when the API reports an error.
     */
    suspend fun scrobble(
        artist: String,
        track: String,
        duration: Int,
        timestamp: Long,
        album: String?,
    ) {
        call("track.scrobble", buildMap {
            put("artist", artist)
            put("track", track)
            put("timestamp", timestamp.toString())
            album?.let { put("album", it) }
            if (duration > 0) put("duration", duration.toString())
        }, authed = true)
    }

    /**
     * Updates the now-playing status (method `track.updateNowPlaying`).
     *
     * @throws LastFMException when the API reports an error.
     */
    suspend fun updateNowPlaying(
        artist: String,
        track: String,
        album: String?,
        duration: Int,
    ) {
        call("track.updateNowPlaying", buildMap {
            put("artist", artist)
            put("track", track)
            album?.let { put("album", it) }
            if (duration > 0) put("duration", duration.toString())
        }, authed = true)
    }

    /**
     * Loves a track (method `track.love`).
     *
     * @throws LastFMException when the API reports an error.
     */
    suspend fun love(track: String, artist: String) {
        call("track.love", buildMap {
            put("artist", artist)
            put("track", track)
        }, authed = true)
    }

    /**
     * Unloves a track (method `track.unlove`).
     *
     * @throws LastFMException when the API reports an error.
     */
    suspend fun unlove(track: String, artist: String) {
        call("track.unlove", buildMap {
            put("artist", artist)
            put("track", track)
        }, authed = true)
    }

    // ── Authentication ──────────────────────────────────────────────────────

    /**
     * Exchanges [username]/[password] for a session (method
     * `auth.getMobileSession`), stores it in [sessionKey] and returns the key.
     *
     * @throws LastFMException on invalid credentials or API errors.
     */
    suspend fun getSession(username: String, password: String): String {
        val key = call("auth.getMobileSession", buildMap {
            put("username", username)
            put("password", password)
        }, authed = false)["session"]
            ?.jsonObject?.get("key")
            ?.jsonPrimitive?.content
            ?: throw LastFMException("Failed to obtain a Last.fm session")
        sessionKey = key
        return key
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private suspend fun call(
        method: String,
        params: Map<String, String>,
        authed: Boolean,
    ): JsonObject {
        val key = apiKey ?: throw LastFMException("LastFM.initialize was never called")
        val sharedSecret = secret ?: throw LastFMException("LastFM.initialize was never called")

        if (authed && sessionKey.isNullOrBlank()) {
            throw LastFMException("Not signed in to Last.fm")
        }

        val form = buildMap {
            put("method", method)
            put("format", "json")
            put("api_key", key)
            if (authed) put("sk", sessionKey!!)
            putAll(params)
        }

        val signed = buildMap {
            putAll(form)
            put("api_sig", signature(form, sharedSecret))
        }

        val body = post(signed)
        val element = runCatching { json.parseToJsonElement(body) }.getOrNull()
            ?: throw LastFMException("Malformed Last.fm response")

        val root = element.jsonObject
        root["error"]?.jsonPrimitive?.content?.let { code ->
            val message = root["message"]?.jsonPrimitive?.content ?: "Unknown Last.fm error"
            throw LastFMException("Last.fm error $code: $message")
        }
        return root
    }

    /**
     * MD5 signature: alphabetical `key` + `value` concatenation of all
     * parameters (except `format`), with the shared secret appended.
     */
    private fun signature(params: Map<String, String>, sharedSecret: String): String {
        val toSign = params.entries
            .filter { it.key != "format" && it.key != "api_sig" && it.key != "callback" }
            .sortedBy { it.key }
            .joinToString("") { "${it.key}${it.value}" }
        return md5(toSign + sharedSecret)
    }

    private fun md5(input: String): String =
        MessageDigest.getInstance("MD5")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private suspend fun post(params: Map<String, String>): String = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder().apply {
            params.forEach { (name, value) -> add(name, value) }
        }.build()

        val request = Request.Builder()
            .url(API_ROOT)
            .post(formBody)
            .header("User-Agent", "Dhun/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                throw LastFMException("Empty Last.fm response (HTTP ${response.code})")
            }
            body
        }
    }
}
