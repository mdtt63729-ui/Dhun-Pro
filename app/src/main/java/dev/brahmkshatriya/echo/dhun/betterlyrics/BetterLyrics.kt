/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.betterlyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Client for the Better Lyrics API (https://lyrics-api.boidu.dev), which serves
 * Apple-Music-style TTML with syllable/word-level timing.
 *
 * Endpoints used:
 *  - `GET /getLyrics?s=<song>&a=<artist>&al=<album>&d=<duration>` — default
 *    provider, returns `{"lyrics": "<ttml>", "provider": "..."}`.
 *  - `GET /ttml/getLyrics` / `GET /kugou/getLyrics` / `GET /legacy/getLyrics` —
 *    provider-specific variants, used to enumerate all lyrics options.
 */
object BetterLyrics {

    /** Optional log sink, wired up by [dev.brahmkshatriya.echo.dhun.lyrics.BetterLyricsProvider]. */
    var logger: ((String) -> Unit)? = null

    private const val BASE_URL = "https://lyrics-api.boidu.dev"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val endpoints = listOf(
        "/getLyrics",
        "/ttml/getLyrics",
        "/kugou/getLyrics",
        "/legacy/getLyrics",
    )

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Fetches TTML lyrics for [title]/[artist] from the default provider.
     *
     * @param durationSeconds track duration in seconds (improves matching).
     * @return [Result] holding the raw TTML document as a string.
     */
    suspend fun getLyrics(
        title: String,
        artist: String,
        album: String?,
        durationSeconds: Int,
    ): Result<String> = runCatching {
        fetch("/getLyrics", title, artist, album, durationSeconds)
            ?: throw IllegalStateException("No lyrics found on Better Lyrics")
    }

    /**
     * Calls [callback] with every distinct lyrics variant found across the
     * Better Lyrics providers (default, ttml, kugou and legacy).
     */
    suspend fun getAllLyrics(
        title: String,
        artist: String,
        album: String?,
        durationSeconds: Int,
        callback: (String) -> Unit,
    ) {
        val seen = mutableSetOf<String>()
        for (endpoint in endpoints) {
            val lyrics = runCatching {
                fetch(endpoint, title, artist, album, durationSeconds)
            }.getOrNull() ?: continue
            if (lyrics.isNotBlank() && seen.add(lyrics)) callback(lyrics)
        }
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private suspend fun fetch(
        endpoint: String,
        title: String,
        artist: String,
        album: String?,
        durationSeconds: Int,
    ): String? {
        val url = (BASE_URL + endpoint).toHttpUrl().newBuilder().apply {
            addQueryParameter("s", title)
            addQueryParameter("a", artist)
            album?.let { addQueryParameter("al", it) }
            if (durationSeconds > 0) addQueryParameter("d", durationSeconds.toString())
        }.build()

        val body = execute(url) ?: return null
        if (body.isBlank()) return null

        // Response: {"lyrics": "<ttml or lrc>", "provider": "..."}
        val lyrics = runCatching {
            val element = json.parseToJsonElement(body).jsonObject
            element["lyrics"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()

        if (lyrics.isNullOrBlank()) {
            logger?.invoke("BetterLyrics: no lyrics from $endpoint")
            return null
        }
        return lyrics
    }

    private suspend fun execute(url: okhttp3.HttpUrl): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Dhun/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger?.invoke("BetterLyrics: HTTP ${response.code} for ${url}")
                    return@use null
                }
                response.body?.string()
            }
        }.getOrNull()
    }
}
