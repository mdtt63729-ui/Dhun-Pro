/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.lrclib

import dev.brahmkshatriya.echo.common.models.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Client for the public LRCLIB lyrics API (https://lrclib.net).
 *
 * Endpoints used:
 *  - `GET /api/get?artist_name=&track_name=&album_name=&duration=` — exact
 *    lookup, returns a single record (404 when not found).
 *  - `GET /api/search?track_name=&artist_name=&album_name=` — fuzzy search,
 *    returns a list of records.
 */
object LrcLib {

    private val hosts = listOf(
        "https://api.lyrics.lrclib.net",
        "https://lrclib.net",
    )

    private const val USER_AGENT = "Dhun/1.0 (https://github.com/brahmkshatriya/echo)"

    /** Allowed mismatch (in seconds) between the requested and result duration. */
    private const val DURATION_TOLERANCE = 5

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // ── DTOs ────────────────────────────────────────────────────────────────

    @Serializable
    data class LyricsDto(
        val id: Long = 0,
        val trackName: String = "",
        val artistName: String = "",
        val albumName: String? = null,
        val duration: Int? = null,
        val instrumental: Boolean = false,
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null,
    ) {
        val best: String? get() = syncedLyrics ?: plainLyrics
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Fetches the best-matching lyrics for [title]/[artist].
     *
     * @param duration track duration in seconds; `-1` disables duration matching.
     * @return [Result] holding the LRC lyrics string (synced when available).
     */
    suspend fun getLyrics(title: String, artist: String, duration: Int): Result<String> =
        runCatching {
            bestMatch(title, artist, duration, null)?.best
                ?: throw IllegalStateException("No lyrics found on LRCLIB")
        }

    /**
     * Calls [callback] with every distinct lyrics variant found for
     * [title]/[artist] (synced variants first, then plain).
     *
     * @param albumName optional album name used to refine the search.
     */
    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        albumName: String?,
        callback: (String) -> Unit,
    ) {
        val results = search(title, artist, albumName)
        val ordered = results
            .sortedBy { dto ->
                if (duration > 0 && dto.duration != null) {
                    abs(dto.duration - duration)
                } else {
                    Int.MAX_VALUE
                }
            }
            .sortedByDescending { it.syncedLyrics != null }

        val seen = mutableSetOf<String>()
        for (dto in ordered) {
            val lyrics = dto.best ?: continue
            if (seen.add(lyrics)) callback(lyrics)
        }
    }

    /**
     * Searches LRCLIB and maps the results onto the common [Lyrics] model
     * (without the lyric bodies attached).
     */
    suspend fun searchLyrics(title: String, artist: String, albumName: String?): List<Lyrics> =
        search(title, artist, albumName).map { dto ->
            Lyrics(
                id = dto.id.toString(),
                title = dto.trackName,
                subtitle = dto.artistName,
                extras = buildMap {
                    dto.albumName?.let { put("albumName", it) }
                    dto.duration?.let { put("duration", it.toString()) }
                    put("instrumental", dto.instrumental.toString())
                    put("synced", (dto.syncedLyrics != null).toString())
                },
            )
        }

    // ── Internals ───────────────────────────────────────────────────────────

    private suspend fun bestMatch(
        title: String,
        artist: String,
        duration: Int,
        albumName: String?,
    ): LyricsDto? {
        // 1) Exact lookup on primary host with duration for precision.
        for (host in hosts) {
            val url = "$host/api/get".toHttpUrl().newBuilder().apply {
                addQueryParameter("track_name", title)
                addQueryParameter("artist_name", artist)
                albumName?.let { addQueryParameter("album_name", it) }
                if (duration > 0) addQueryParameter("duration", duration.toString())
            }.build()

            val body = execute(url) ?: continue
            if (body.isBlank()) continue
            val dto = runCatching { json.decodeFromString<LyricsDto>(body) }.getOrNull()
            if (dto != null && dto.best != null) return dto
        }

        // 2) Fuzzy fallback.
        val results = search(title, artist, albumName)
        if (results.isEmpty()) return null
        if (duration <= 0) return results.firstOrNull { it.best != null }
        return results
            .filter { it.best != null && it.duration != null && abs(it.duration - duration) <= DURATION_TOLERANCE }
            .minByOrNull { abs((it.duration ?: 0) - duration) }
            ?: results.firstOrNull { it.best != null }
    }

    private suspend fun search(title: String, artist: String, albumName: String?): List<LyricsDto> {
        for (host in hosts) {
            val url = "$host/api/search".toHttpUrl().newBuilder().apply {
                addQueryParameter("track_name", title)
                addQueryParameter("artist_name", artist)
                albumName?.let { addQueryParameter("album_name", it) }
            }.build()

            val body = execute(url) ?: continue
            if (body.isBlank()) continue
            val results = runCatching {
                json.decodeFromString<List<LyricsDto>>(body)
            }.getOrNull() ?: continue
            if (results.isNotEmpty()) return results
        }
        return emptyList()
    }

    private suspend fun execute(url: okhttp3.HttpUrl): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
            }
        }.getOrNull()
    }
}
