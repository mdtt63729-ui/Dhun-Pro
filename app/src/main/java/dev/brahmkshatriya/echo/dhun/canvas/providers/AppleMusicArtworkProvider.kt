/*
 * Dhun Project (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.canvas.providers

import dev.brahmkshatriya.echo.dhun.canvas.CanvasHttp
import dev.brahmkshatriya.echo.dhun.canvas.models.CanvasArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves animated "canvas" artwork for a song via Apple Music / iTunes.
 *
 * How it works, step by step:
 *  1. The song is looked up on the public **iTunes Search API**
 *     (`https://itunes.apple.com/search`) — no authentication required — using
 *     a couple of term orderings ("artist song" and "song artist").
 *  2. The best result is chosen by comparing normalized titles / artists
 *     (and album, when provided).
 *  3. From the result's `artworkUrl100` a static artwork URL is derived by the
 *     usual mzstatic size-token substitution (`100x100bb.jpg` -> `1200x1200bb.jpg`).
 *  4. Candidate animated-URLs are derived from the artwork URL by the known
 *     mzstatic host/path transformations (image -> video asset). Every
 *     candidate is **verified with a real HTTP request** (see
 *     [CanvasHttp.verifyMediaUrl]) before being returned, so a pattern that
 *     Apple does not serve for a given album simply yields no animation and the
 *     resolver falls through to the next provider.
 *
 * All failures are swallowed and logged; the provider never throws, it just
 * returns `null` when nothing could be resolved.
 */
object AppleMusicArtworkProvider {

    private const val TAG = "AppleMusicArtworkProvider"
    private const val SEARCH_ENDPOINT = "https://itunes.apple.com/search"
    private const val SEARCH_LIMIT = 25

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    /**
     * Artwork bases (the mzstatic path without the size token) for which we
     * already know that no animated variant is served. Avoids re-verifying
     * dead candidates on every song change.
     */
    private val knownAnimationMisses = ConcurrentHashMap<String, Boolean>()

    // ------------------------------------------------------------------
    // iTunes Search API DTOs (only the fields we consume)
    // ------------------------------------------------------------------

    @Serializable
    private data class ITunesSearchResponse(
        val resultCount: Int = 0,
        val results: List<ITunesResult> = emptyList(),
    )

    @Serializable
    private data class ITunesResult(
        val wrapperType: String? = null,
        val trackId: Long? = null,
        val collectionId: Long? = null,
        val trackName: String? = null,
        val artistName: String? = null,
        val collectionName: String? = null,
        val artworkUrl100: String? = null,
    )

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Resolves the animated artwork for `song` by `artist`.
     *
     * @param song   Song title (already normalized by the caller, but raw values work too).
     * @param artist Artist name.
     * @param album  Album name, when known — used only to disambiguate search results.
     * @return a [CanvasArtwork] with at least one *verified* animation URL, or
     *         `null` when the track could not be resolved or no animated
     *         variant is served.
     */
    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        if (song.isBlank() || artist.isBlank()) return@withContext null

        val terms = linkedSetOf(
            "$artist $song",
            "$song $artist",
        )
        val candidates = terms.flatMap { term -> search(term).orEmpty() }
        val best = pickBestMatch(candidates, song, artist, album)
            ?: run {
            Timber.d("$TAG: no iTunes match for '$song' by '$artist'")
            return@withContext null
        }

        val artworkUrl = best.artworkUrl100?.replaceFirst("http://", "https://")
            ?: return@withContext null

        // Fast path: we already know this artwork has no animated variant.
        val base = artworkBase(artworkUrl) ?: return@withContext null
        if (knownAnimationMisses.containsKey(base)) {
            return@withContext null
        }

        val animated = firstVerifiedAnimatedUrl(artworkUrl)
        if (animated == null) {
            knownAnimationMisses[base] = true
            Timber.d("$TAG: no animated variant for $artworkUrl")
            return@withContext null
        }

        CanvasArtwork(
            id = best.trackId?.toString() ?: best.collectionId?.toString(),
            songTitle = best.trackName ?: song,
            artist = best.artistName ?: artist,
            album = best.collectionName ?: album,
            animated = animated,
            videoUrl = animated.takeIf { it.contains(".mp4", ignoreCase = true) },
            staticUrl = upscaleArtwork(artworkUrl),
            provider = CanvasArtwork.PROVIDER_APPLE_MUSIC,
            type = CanvasArtwork.TYPE_VIDEO,
        )
    }

    /**
     * Derives candidate animated URLs from a static mzstatic artwork URL and
     * returns the first one that Apple actually serves, or `null`.
     */
    suspend fun firstVerifiedAnimatedUrl(artworkUrl: String): String? {
        for (candidate in animatedCandidates(artworkUrl)) {
            if (CanvasHttp.verifyMediaUrl(candidate)) return candidate
        }
        return null
    }

    // ------------------------------------------------------------------
    // Search + matching
    // ------------------------------------------------------------------

    private fun search(term: String): List<ITunesResult>? {
        val url = "$SEARCH_ENDPOINT?term=${urlEncode(term)}" +
            "&entity=song&media=music&limit=$SEARCH_LIMIT&country=US"
        return CanvasHttp.runCatchingRequest(TAG) { client ->
            client.newCall(
                Request.Builder()
                    .url(url)
                    .header("User-Agent", "Dhun/1.0 (canvas resolver)")
                    .get()
                    .build(),
            ).execute().use { response ->
                if (!response.isSuccessful) return@runCatchingRequest null
                val body = response.body?.string() ?: return@runCatchingRequest null
                json.decodeFromString(ITunesSearchResponse.serializer(), body).results
            }
        }
    }

    private fun pickBestMatch(
        results: List<ITunesResult>,
        song: String,
        artist: String,
        album: String?,
    ): ITunesResult? {
        val targetSong = normalize(song)
        val targetArtist = normalize(artist)
        val targetAlbum = album?.takeIf { it.isNotBlank() }?.let(::normalize)

        return results
            .mapNotNull { result ->
                val name = result.trackName?.let(::normalize) ?: return@mapNotNull null
                val artistName = result.artistName?.let(::normalize) ?: return@mapNotNull null
                if (name.isBlank() || artistName.isBlank()) return@mapNotNull null

                var score = 0
                if (name == targetSong) score += 4 else if (name.contains(targetSong)) score += 2
                if (artistName == targetArtist) score += 4 else if (artistName.contains(targetArtist)) score += 2
                if (targetAlbum != null) {
                    val collection = result.collectionName?.let(::normalize)
                    if (collection != null) {
                        if (collection == targetAlbum) score += 3
                        else if (collection.contains(targetAlbum)) score += 1
                    }
                }
                if (score >= 6) result to score else null
            }
            .maxByOrNull { it.second }
            ?.first
    }

    // ------------------------------------------------------------------
    // URL manipulation
    // ------------------------------------------------------------------

    /**
     * Builds the list of animated-URL candidates for an mzstatic artwork URL.
     *
     * Typical artwork URL shape:
     * `https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/ab/cd/ef/<uuid>/100x100bb.jpg`
     *
     * Candidates tried, in order (each is HTTP-verified before use):
     *  1. same host/path with `image/thumb` -> `video/thumb`, size token -> `source.m3u8`
     *  2. same as (1) but with the original size numbers kept, `.m3u8` suffix
     *  3. same as (1) with a `source.mp4` suffix (progressive fallback)
     */
    private fun animatedCandidates(artworkUrl: String): List<String> {
        val candidates = mutableListOf<String>()
        val base = artworkBase(artworkUrl) ?: return candidates
        // e.g. https://is1-ssl.mzstatic.com/video/thumb/Music125/v4/ab/cd/ef/<uuid>
        val videoBase = base.replaceFirst("/image/thumb/", "/video/thumb/")
        candidates += "$videoBase/source.m3u8"
        sizeToken(artworkUrl)?.let { size -> candidates += "$videoBase/$size.m3u8" }
        candidates += "$videoBase/source.mp4"
        return candidates.distinct()
    }

    /**
     * `https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/ab/cd/ef/<uuid>/100x100bb.jpg`
     *  -> `https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/ab/cd/ef/<uuid>`
     */
    private fun artworkBase(artworkUrl: String): String? {
        val index = artworkUrl.lastIndexOf('/')
        if (index <= 0) return null
        return artworkUrl.substring(0, index)
    }

    /** Extracts the `100x100` part of the size token, when present. */
    private fun sizeToken(artworkUrl: String): String? {
        val token = artworkUrl.substringAfterLast('/', "")
        val match = Regex("^(\\d+x\\d+)").find(token) ?: return null
        return match.groupValues[1]
    }

    /** `.../100x100bb.jpg` -> `.../1200x1200bb.jpg` (falls back to the input). */
    private fun upscaleArtwork(artworkUrl: String): String {
        val upscaled = artworkUrl.replace(
            Regex("\\d+x\\d+(bb|bf|cc)\\.(jpg|jpeg|png)", RegexOption.IGNORE_CASE),
        ) { "1200x1200bb.jpg" }
        return upscaled.ifEmpty { artworkUrl }
    }

    private fun urlEncode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
        .trim()
}
