/*
 * Dhun Project — Paxsenix provider adapted from Echo Music.
 * Licensed under GPL-3.0.
 */
package dev.brahmkshatriya.echo.dhun.lyrics

import android.content.Context
import dev.brahmkshatriya.echo.dhun.constants.EnablePaxsenixKey
import dev.brahmkshatriya.echo.dhun.utils.dataStore
import dev.brahmkshatriya.echo.dhun.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

private object PaxsenixClient {
    private val client by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) { connectTimeoutMillis = 4_000; requestTimeoutMillis = 5_000; socketTimeoutMillis = 5_000 }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
            expectSuccess = false
        }
    }

    @Serializable
    private data class SearchResult(
        val id: String,
        val songName: String? = null,
        val trackName: String? = null,
        val artistName: String? = null,
        val duration: Int? = null,
    ) {
        val title: String get() = trackName ?: songName ?: ""
        val artist: String get() = artistName ?: ""
    }

    @Serializable
    private data class Word(val text: String, val timestamp: Long, val endtime: Long)

    @Serializable
    private data class Line(
        val timestamp: Long,
        val text: List<Word> = emptyList(),
        val background: Boolean = false,
        val oppositeTurn: Boolean = false,
    )

    @Serializable
    private data class LyricsResponse(
        val type: String? = null,
        val content: List<Line> = emptyList(),
        val elrc: String? = null,
        val elrcMultiPerson: String? = null,
        val ttmlContent: String? = null,
        val plain: String? = null,
    )

    suspend fun getLyrics(title: String, artist: String, duration: Int, album: String?): Result<String> = runCatching {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        val queries = buildList {
            add("$cleanedTitle $cleanedArtist")
            add(cleanedTitle)
            if (!album.isNullOrBlank()) add("$cleanedTitle $cleanedArtist $album")
        }
        var scored = emptyList<Pair<SearchResult, Double>>()
        for (query in queries) {
            val results = search(query)
            scored = score(results, title, artist, duration)
            if (scored.isNotEmpty()) break
        }
        if (scored.isEmpty()) throw IllegalStateException("No Paxsenix track match")

        val scope = CoroutineScope(Dispatchers.IO)
        val jobs = scored.take(5).map { result -> scope.async { fetchLyrics(result.first.id) } }
        var fallback: String? = null
        try {
            val remaining = jobs.toMutableList()
            while (remaining.isNotEmpty()) {
                val value = select { remaining.forEach { it.onAwait { response -> response } } }
                remaining.removeAll { it.isCompleted }
                val lyrics = value?.toAppLyrics()
                if (!lyrics.isNullOrBlank()) {
                    if (lyrics.contains("<") && lyrics.contains(">")) return@runCatching lyrics
                    if (fallback == null) fallback = lyrics
                }
            }
        } finally { scope.coroutineContext.cancelChildren() }
        fallback ?: throw IllegalStateException("No Paxsenix lyrics")
    }

    suspend fun getAllLyrics(title: String, artist: String, duration: Int, album: String?, callback: (String) -> Unit) {
        val scored = score(search("${cleanTitle(title)} ${cleanArtist(artist)}"), title, artist, duration).take(3)
        val scope = CoroutineScope(Dispatchers.IO)
        val jobs = scored.map { scope.async { fetchLyrics(it.first.id)?.toAppLyrics() } }
        val emitted = HashSet<String>()
        try {
            val remaining = jobs.toMutableList()
            while (remaining.isNotEmpty()) {
                val lyrics = select { remaining.forEach { it.onAwait { value -> value } } }
                remaining.removeAll { it.isCompleted }
                if (!lyrics.isNullOrBlank() && emitted.add(lyrics)) callback(lyrics)
            }
        } finally { scope.coroutineContext.cancelChildren() }
    }

    private suspend fun search(query: String): List<SearchResult> = runCatching {
        client.get("https://lyrics.paxsenix.org/apple-music/search") { parameter("q", query) }
            .takeIf { it.status.value in 200..299 }?.body<List<SearchResult>>() ?: emptyList()
    }.getOrDefault(emptyList())

    private suspend fun fetchLyrics(id: String): LyricsResponse? = runCatching {
        client.get("https://lyrics.paxsenix.org/apple-music/lyrics") {
            parameter("id", id)
            header("Accept", "application/json")
        }.takeIf { it.status.value in 200..299 }?.body<LyricsResponse>()
    }.getOrNull()

    private fun score(results: List<SearchResult>, title: String, artist: String, duration: Int): List<Pair<SearchResult, Double>> {
        val cleanT = title.replace(Regex("\\s*\\(.*?\\)|\\s*\\[.*?\\]"), "").lowercase().trim()
        val cleanA = cleanArtist(artist).lowercase()
        return results.map { result ->
            var score = 0.0
            result.duration?.let { d ->
                val diff = abs(d - duration * 1000)
                score += when { diff <= 2_000 -> 100.0; diff <= 5_000 -> 50.0; diff <= 10_000 -> 10.0; else -> -50.0 }
            }
            val rt = result.title.replace(Regex("\\s*\\(.*?\\)|\\s*\\[.*?\\]"), "").lowercase().trim()
            score += when { rt == cleanT -> 80.0; rt.contains(cleanT) || cleanT.contains(rt) -> 40.0; else -> 0.0 }
            val ra = result.artist.lowercase()
            score += if (ra.contains(cleanA)) 50.0 else if (cleanA.split(Regex("\\s+")).any { it.length > 2 && ra.contains(it) }) 25.0 else 0.0
            result to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }.take(10)
    }

    private fun cleanTitle(value: String): String = value.trim().replace(Regex("\\s*\\(.*?(official|video|audio|lyrics|remix|live|acoustic|version|edit|radio).*?\\)", RegexOption.IGNORE_CASE), "").trim()
    private fun cleanArtist(value: String): String = value.trim().split(Regex("\\s+(?:&|and|feat\\.?|ft\\.?|with|x)\\s+", RegexOption.IGNORE_CASE)).firstOrNull()?.trim().orEmpty()

    private fun LyricsResponse.toAppLyrics(): String? {
        ttmlContent?.takeIf { it.isNotBlank() }?.let { return it }
        elrcMultiPerson?.takeIf { it.isNotBlank() }?.let { return it }
        elrc?.takeIf { it.isNotBlank() }?.let { return it }
        plain?.takeIf { it.isNotBlank() }?.let { return it }
        if (content.isEmpty()) return null
        if (type != "Syllable") return content.map { it.text.joinToString(" ") { w -> w.text } }.filter { it.isNotBlank() }.joinToString("\n")
        return content.mapNotNull { line ->
            val text = line.text.joinToString(" ") { it.text }.trim()
            if (text.isBlank()) return@mapNotNull null
            val t = line.timestamp
            val main = "[%02d:%02d.%02d]%s%s".format(t / 60000, (t / 1000) % 60, (t % 1000) / 10, if (line.background) "{bg}" else "", text)
            val words = line.text.joinToString("|") { w -> "${w.text}:${w.timestamp / 1000.0}:${w.endtime / 1000.0}" }
            if (words.isBlank()) main else "$main\n<$words>"
        }.joinToString("\n")
    }
}

object PaxSenixLyricsProvider : LyricsProvider {
    override val name = "Paxsenix"
    override fun isEnabled(context: Context): Boolean = context.dataStore[EnablePaxsenixKey] ?: true
    override suspend fun getLyrics(id: String, title: String, artist: String, album: String?, duration: Int): Result<String> = PaxsenixClient.getLyrics(title, artist, duration, album)
    override suspend fun getAllLyrics(id: String, title: String, artist: String, album: String?, duration: Int, callback: (String) -> Unit) = PaxsenixClient.getAllLyrics(title, artist, duration, album, callback)
}
