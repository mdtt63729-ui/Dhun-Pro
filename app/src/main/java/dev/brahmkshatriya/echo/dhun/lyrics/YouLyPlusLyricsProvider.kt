/*
 * Dhun Project — YouLyPlus / LyricsPlus provider adapted from Echo Music.
 * Licensed under GPL-3.0.
 */
package dev.brahmkshatriya.echo.dhun.lyrics

import android.content.Context
import dev.brahmkshatriya.echo.dhun.constants.EnableYouLyPlusKey
import dev.brahmkshatriya.echo.dhun.utils.dataStore
import dev.brahmkshatriya.echo.dhun.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference

private object YouLyPlusClient {
    private val servers = listOf(
        "https://lyricsplus.prjktla.my.id",
        "https://lyricsplus.atomix.one",
        "https://lyricsplus.binimum.org",
        "https://lyricsplus.prjktla.workers.dev",
        "https://lyricsplus-seven.vercel.app",
        "https://lyrics-plus-backend.vercel.app",
    )
    private val lastWorking = AtomicReference<String?>(null)
    private val client by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 3_000
                requestTimeoutMillis = 8_000
                socketTimeoutMillis = 8_000
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
            expectSuccess = false
        }
    }

    @Serializable
    private data class Response(
        val syncedLyrics: String? = null,
        val plainLyrics: String? = null,
        val lyrics: List<Line>? = null,
    )

    @Serializable
    private data class Line(
        val text: String? = null,
        val time: Long? = null,
        val syllabus: List<Syllable>? = null,
    )

    @Serializable
    private data class Syllable(
        val text: String? = null,
        val time: Long? = null,
        val duration: Long? = null,
        val isBackground: Boolean? = null,
    )

    suspend fun getLyrics(title: String, artist: String, duration: Int, album: String?, id: String): Result<String> = runCatching {
        val ordered = lastWorking.get()?.let { listOf(it) + servers.filterNot { s -> s == it } } ?: servers
        val scope = CoroutineScope(Dispatchers.IO)
        val jobs = ordered.map { server ->
            server to scope.async { fetch(server, title, artist, duration, album, id) }
        }
        try {
            val remaining = jobs.toMutableList()
            while (remaining.isNotEmpty()) {
                val (server, response) = select {
                    remaining.forEach { (srv, job) -> job.onAwait { srv to it } }
                }
                remaining.removeAll { it.first == server }
                val lyrics = response?.toRichLyrics()
                if (!lyrics.isNullOrBlank()) {
                    lastWorking.set(server)
                    return@runCatching lyrics
                }
            }
            throw IllegalStateException("No lyrics found from YouLyPlus")
        } finally {
            scope.coroutineContext.cancelChildren()
        }
    }

    suspend fun getAllLyrics(title: String, artist: String, duration: Int, album: String?, id: String, callback: (String) -> Unit) {
        val ordered = lastWorking.get()?.let { listOf(it) + servers.filterNot { s -> s == it } } ?: servers
        val scope = CoroutineScope(Dispatchers.IO)
        val jobs = ordered.map { server -> scope.async { fetch(server, title, artist, duration, album, id)?.toRichLyrics() } }
        val emitted = LinkedHashSet<String>()
        try {
            val remaining = jobs.toMutableList()
            while (remaining.isNotEmpty()) {
                val lyrics = select { remaining.forEach { job -> job.onAwait { it } } }
                remaining.removeAll { it.isCompleted }
                if (!lyrics.isNullOrBlank() && emitted.add(lyrics)) callback(lyrics)
            }
        } finally {
            scope.coroutineContext.cancelChildren()
        }
    }

    private suspend fun fetch(server: String, title: String, artist: String, duration: Int, album: String?, id: String): Response? = runCatching {
        client.get("${server.trimEnd('/')}/v2/lyrics/get") {
            parameter("title", title)
            parameter("artist", artist)
            parameter("duration", duration)
            if (!album.isNullOrBlank()) parameter("album", album)
            if (id.isNotBlank()) parameter("id", id)
        }.takeIf { it.status.value in 200..299 }?.body<Response>()
    }.getOrNull()

    private fun Response.toRichLyrics(): String? {
        syncedLyrics?.takeIf { it.isNotBlank() }?.let { return it }
        lyrics?.takeIf { it.isNotEmpty() }?.let { lines ->
            return lines.mapNotNull { line ->
                val time = line.time ?: return@mapNotNull null
                val text = line.text.orEmpty().trim()
                if (text.isBlank()) return@mapNotNull null
                val min = time / 60000
                val sec = (time / 1000) % 60
                val cs = (time % 1000) / 10
                val main = "[%02d:%02d.%02d]%s".format(min, sec, cs, text)
                val words = line.syllabus.orEmpty().mapNotNull { word ->
                    val wt = word.time ?: return@mapNotNull null
                    val start = wt / 1000.0
                    val end = (wt + (word.duration ?: 0L)) / 1000.0
                    "${word.text.orEmpty()}:${"%.3f".format(start)}:${"%.3f".format(end)}"
                }
                if (words.isEmpty()) main else "$main\n<${words.joinToString("|")}>"
            }.joinToString("\n").takeIf { it.isNotBlank() }
        }?.let { return it }
        return plainLyrics?.takeIf { it.isNotBlank() }
    }
}

object YouLyPlusLyricsProvider : LyricsProvider {
    override val name = "YouLyPlus"
    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableYouLyPlusKey] ?: true

    override suspend fun getLyrics(id: String, title: String, artist: String, album: String?, duration: Int): Result<String> =
        YouLyPlusClient.getLyrics(title, artist, duration, album, id)

    override suspend fun getAllLyrics(id: String, title: String, artist: String, album: String?, duration: Int, callback: (String) -> Unit) =
        YouLyPlusClient.getAllLyrics(title, artist, duration, album, id, callback)
}
