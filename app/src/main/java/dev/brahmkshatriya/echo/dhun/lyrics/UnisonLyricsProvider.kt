/*
 * Dhun Project — Unison provider adapted from Echo Music.
 * Licensed under GPL-3.0.
 */
package dev.brahmkshatriya.echo.dhun.lyrics

import android.content.Context
import dev.brahmkshatriya.echo.dhun.constants.UnisonLyricsEnabledKey
import dev.brahmkshatriya.echo.dhun.utils.dataStore
import dev.brahmkshatriya.echo.dhun.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private object UnisonClient {
    private const val BASE = "https://unison.boidu.dev/"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) { connectTimeoutMillis = 15_000; requestTimeoutMillis = 20_000; socketTimeoutMillis = 20_000 }
            expectSuccess = false
        }
    }

    @Serializable
    private data class Entry(
        val id: Long = 0,
        val videoId: String? = null,
        val song: String = "",
        val artist: String = "",
        val lyrics: String = "",
        val format: String = "",
        val syncType: String = "",
        val score: Double = 0.0,
        val effectiveScore: Double = 0.0,
        val voteCount: Int = 0,
        val confidence: String = "low",
        val language: String? = null,
    )
    @Serializable private data class Response(val success: Boolean, val data: Entry? = null)
    @Serializable private data class SearchResponse(val success: Boolean, val data: List<Entry>? = null)

    suspend fun getLyrics(id: String, title: String, artist: String, album: String?, duration: Int): Result<String> = runCatching {
        if (id.isNotBlank()) fetchById(id)?.lyrics?.takeIf { it.isNotBlank() }?.let { return@runCatching it }
        fetchByMetadata(title, artist, album, duration)?.lyrics?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Unison lyrics unavailable")
    }

    suspend fun getAllLyrics(id: String, title: String, artist: String, album: String?, duration: Int, callback: (String) -> Unit) {
        val results = search(title, artist, album, duration).take(5)
        if (results.isNotEmpty()) {
            results.forEach { callback(it.lyrics) }
        } else {
            getLyrics(id, title, artist, album, duration).onSuccess(callback)
        }
    }

    private suspend fun fetchById(id: String): Entry? = runCatching {
        val response = client.get("${BASE}lyrics") { parameter("v", id) }
        if (!response.status.isSuccess()) null else json.decodeFromString<Response>(response.bodyAsText()).data
    }.getOrNull()

    private suspend fun fetchByMetadata(title: String, artist: String, album: String?, duration: Int): Entry? = runCatching {
        val response = client.get("${BASE}lyrics") {
            parameter("song", title.trim())
            parameter("artist", artist.trim())
            if (!album.isNullOrBlank()) parameter("album", album.trim())
            if (duration > 0) parameter("duration", duration)
        }
        if (!response.status.isSuccess()) null else json.decodeFromString<Response>(response.bodyAsText()).data
    }.getOrNull()

    private suspend fun search(title: String, artist: String, album: String?, duration: Int): List<Entry> = runCatching {
        val response = client.get("${BASE}lyrics/search") {
            parameter("song", title.trim())
            parameter("artist", artist.trim())
            if (!album.isNullOrBlank()) parameter("album", album.trim())
            if (duration > 0) parameter("duration", duration)
        }
        if (!response.status.isSuccess()) emptyList() else json.decodeFromString<SearchResponse>(response.bodyAsText()).data.orEmpty().filter { it.lyrics.isNotBlank() }
    }.getOrDefault(emptyList())
}

object UnisonLyricsProvider : LyricsProvider {
    override val name = "Unison"
    override fun isEnabled(context: Context): Boolean = context.dataStore[UnisonLyricsEnabledKey] ?: true
    override suspend fun getLyrics(id: String, title: String, artist: String, album: String?, duration: Int): Result<String> = UnisonClient.getLyrics(id, title, artist, album, duration)
    override suspend fun getAllLyrics(id: String, title: String, artist: String, album: String?, duration: Int, callback: (String) -> Unit) = UnisonClient.getAllLyrics(id, title, artist, album, duration, callback)
}
