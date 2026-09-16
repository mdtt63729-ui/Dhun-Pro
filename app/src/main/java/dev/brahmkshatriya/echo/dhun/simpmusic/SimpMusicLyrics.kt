/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */


package dev.brahmkshatriya.echo.dhun.simpmusic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Client for the SimpMusic community lyrics API (api-lyrics.simpmusic.org),
 * keyed by YouTube video id. Mirrors the shape used by the SimpMusic project.
 */
object SimpMusicLyrics {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    private data class SimpMusicResponse(
        // The API has used both spellings across versions; tolerate either.
        val syncedLyric: String? = null,
        val syncedLyrics: String? = null,
        val plainLyric: String? = null,
        val plainLyrics: String? = null,
        val source: String? = null,
    )

    @Serializable
    private data class TranslatedResponse(
        val translations: List<Translation> = emptyList()
    )

    @Serializable
    private data class Translation(
        val language: String? = null,
        val syncedLyric: String? = null,
        val syncedLyrics: String? = null,
        val plainLyric: String? = null,
        val plainLyrics: String? = null,
    )

    private val SimpMusicResponse.synced: String?
        get() = syncedLyric ?: syncedLyrics

    private val SimpMusicResponse.plain: String?
        get() = plainLyric ?: plainLyrics

    private val Translation.synced: String?
        get() = syncedLyric ?: syncedLyrics

    private val Translation.plain: String?
        get() = plainLyric ?: plainLyrics

    private fun fetch(url: String): String? = runCatching {
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body?.string()
        }
    }.getOrNull()

    /**
     * Primary lyrics for a track: synced (LRC) when available, falling back to
     * plain lyrics. Returns [Result.failure] when nothing could be fetched.
     */
    suspend fun getLyrics(videoId: String, duration: Int): Result<String> =
        withContext(Dispatchers.IO) {
            val body = fetch("https://api-lyrics.simpmusic.org/v1/$videoId")
                ?: return@withContext Result.failure(IllegalStateException("SimpMusic lyrics unavailable"))
            val parsed = runCatching { json.decodeFromString<SimpMusicResponse>(body) }.getOrNull()
            val lyrics = parsed?.synced ?: parsed?.plain
                ?: return@withContext Result.failure(IllegalStateException("No SimpMusic lyrics for $videoId"))
            Result.success(lyrics)
        }

    /**
     * All available lyrics variants for a track, delivered through [callback]
     * as they resolve: synced lyrics first (when present), then plain lyrics,
     * then any community translations.
     */
    suspend fun getAllLyrics(
        videoId: String,
        duration: Int,
        callback: (String) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val body = fetch("https://api-lyrics.simpmusic.org/v1/$videoId")
        if (body != null) {
            val parsed = runCatching { json.decodeFromString<SimpMusicResponse>(body) }.getOrNull()
            parsed?.synced?.let(callback)
            parsed?.plain?.let { if (it != parsed.synced) callback(it) }

            // Community translations.
            runCatching {
                fetch("https://api-lyrics.simpmusic.org/v1/translated/$videoId")
            }.getOrNull()?.let { translationBody ->
                runCatching {
                    json.decodeFromString<TranslatedResponse>(translationBody)
                }.getOrNull()?.translations?.forEach { translation ->
                    translation.synced?.let(callback)
                    translation.plain?.let { if (it != translation.synced) callback(it) }
                }
            }
        }
    }
}
