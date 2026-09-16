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
 * Resolves animated "canvas" artwork for a song from TIDAL.
 *
 * TIDAL exposes animated album artwork as short looping videos hosted on
 * `resources.tidal.com`. Given an album cover id (a dashed UUID), the video is
 * available at:
 *
 *     https://resources.tidal.com/videos/<coverId with '-' replaced by '/'>/<size>x<size>.mp4
 *
 * and the original-quality variant at `.../origin.mp4`. The static cover is at
 * `https://resources.tidal.com/images/<coverId>/<size>x<size>.jpg`.
 *
 * Track search runs against the legacy `https://api.tidal.com/v1/search`
 * endpoint, which requires an application token sent as the `x-tidal-token`
 * header. Because public tokens rotate and the endpoint is not officially
 * documented for third-party use, the token is **not hardcoded**: set
 * [apiToken] at app start (e.g. from a remote config or a user setting) to
 * enable the provider. While [apiToken] is `null` the provider is a graceful
 * no-op that returns `null` and lets the resolver fall through.
 *
 * Every generated animation URL is verified with a real HTTP request before it
 * is returned (see [CanvasHttp.verifyMediaUrl]), so covers that have no
 * uploaded video simply yield `null` instead of a broken canvas.
 */
object TidalCanvasProvider {

    private const val TAG = "TidalCanvasProvider"
    private const val SEARCH_ENDPOINT = "https://api.tidal.com/v1/search"
    private const val RESOURCES_VIDEOS = "https://resources.tidal.com/videos"
    private const val RESOURCES_IMAGES = "https://resources.tidal.com/images"
    private const val SEARCH_LIMIT = 25
    private const val DEFAULT_VIDEO_SIZE = 1280
    private const val DEFAULT_IMAGE_SIZE = 1280

    /**
     * The `x-tidal-token` application token used for api.tidal.com requests.
     * Set this at startup to enable the provider; `null` (default) keeps it
     * disabled. This is a public client token, not a user credential.
     */
    @Volatile
    var apiToken: String? = null

    /** Storefront used for the search; TIDAL catalog ids are storefront-scoped. */
    @Volatile
    var countryCode: String = "US"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    /** Album cover ids already known to have no animated variant. */
    private val knownAnimationMisses = ConcurrentHashMap<String, Boolean>()

    // ------------------------------------------------------------------
    // TIDAL v1 search DTOs (only the fields we consume)
    // ------------------------------------------------------------------

    @Serializable
    private data class TidalSearchResponse(
        val tracks: TidalTracks? = null,
    )

    @Serializable
    private data class TidalTracks(
        val items: List<TidalTrack> = emptyList(),
    )

    @Serializable
    private data class TidalTrack(
        val id: Long? = null,
        val title: String? = null,
        val artist: TidalArtist? = null,
        val album: TidalAlbum? = null,
    )

    @Serializable
    private data class TidalArtist(
        val id: Long? = null,
        val name: String? = null,
    )

    @Serializable
    private data class TidalAlbum(
        val id: Long? = null,
        val title: String? = null,
        val cover: String? = null,
    )

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Resolves the animated artwork for `song` by `artist` from TIDAL.
     *
     * @param song   Song title.
     * @param artist Artist name.
     * @param album  Album name, when known — used to disambiguate results.
     * @return a [CanvasArtwork] with a *verified* animation URL, or `null`
     *         when the provider is disabled ([apiToken] is null), the track
     *         cannot be found, or the album has no animated cover.
     */
    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        if (song.isBlank() || artist.isBlank()) return@withContext null

        val token = apiToken ?: run {
            Timber.d("$TAG: disabled, no api token configured")
            return@withContext null
        }

        val tracks = search(token, "$artist $song") + search(token, "$song $artist")
        val best = pickBestMatch(tracks, song, artist, album)
            ?: run {
                Timber.d("$TAG: no TIDAL match for '$song' by '$artist'")
                return@withContext null
            }

        val coverId = best.album?.cover?.takeIf { it.isNotBlank() }
            ?: return@withContext null

        if (knownAnimationMisses.containsKey(coverId)) {
            return@withContext null
        }

        val animatedUrl = animatedArtworkUrl(coverId, DEFAULT_VIDEO_SIZE)
        if (!CanvasHttp.verifyMediaUrl(animatedUrl)) {
            knownAnimationMisses[coverId] = true
            Timber.d("$TAG: no animated cover for $coverId")
            return@withContext null
        }

        CanvasArtwork(
            id = best.id?.toString(),
            songTitle = best.title ?: song,
            artist = best.artist?.name ?: artist,
            album = best.album?.title ?: album,
            animated = animatedUrl,
            videoUrl = originArtworkUrl(coverId)?.takeIf { CanvasHttp.verifyMediaUrl(it) },
            staticUrl = staticArtworkUrl(coverId, DEFAULT_IMAGE_SIZE),
            provider = CanvasArtwork.PROVIDER_TIDAL,
            type = CanvasArtwork.TYPE_VIDEO,
        )
    }

    // ------------------------------------------------------------------
    // URL builders
    // ------------------------------------------------------------------

    /**
     * Builds the looping-video URL for a TIDAL album cover id.
     * `coverId` is the dashed uuid from the API (e.g. `abcd1234-...`);
     * the resource path uses it with dashes replaced by slashes.
     */
    fun animatedArtworkUrl(coverId: String, size: Int = DEFAULT_VIDEO_SIZE): String =
        "$RESOURCES_VIDEOS/${coverId.replace("-", "/")}/${size}x$size.mp4"

    /** Original-quality variant of the looping video, when present. */
    fun originArtworkUrl(coverId: String): String =
        "$RESOURCES_VIDEOS/${coverId.replace("-", "/")}/origin.mp4"

    /** Static cover image URL for a TIDAL album cover id. */
    fun staticArtworkUrl(coverId: String, size: Int = DEFAULT_IMAGE_SIZE): String =
        "$RESOURCES_IMAGES/${coverId.replace("-", "/")}/${size}x$size.jpg"

    // ------------------------------------------------------------------
    // Search + matching
    // ------------------------------------------------------------------

    private fun search(token: String, query: String): List<TidalTrack> {
        val url = "$SEARCH_ENDPOINT" +
            "?query=${urlEncode(query)}" +
            "&types=TRACKS&limit=$SEARCH_LIMIT&offset=0" +
            "&countryCode=$countryCode"
        return CanvasHttp.runCatchingRequest(TAG) { client ->
            client.newCall(
                Request.Builder()
                    .url(url)
                    .header("x-tidal-token", token)
                    .header("Accept", "application/json")
                    .header("User-Agent", "Dhun/1.0 (canvas resolver)")
                    .get()
                    .build(),
            ).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.w("$TAG: search failed with HTTP ${response.code}")
                    return@runCatchingRequest emptyList()
                }
                val body = response.body?.string() ?: return@runCatchingRequest emptyList()
                json.decodeFromString(TidalSearchResponse.serializer(), body)
                    .tracks?.items.orEmpty()
            }
        }.orEmpty()
    }

    private fun pickBestMatch(
        tracks: List<TidalTrack>,
        song: String,
        artist: String,
        album: String?,
    ): TidalTrack? {
        val targetSong = normalize(song)
        val targetArtist = normalize(artist)
        val targetAlbum = album?.takeIf { it.isNotBlank() }?.let(::normalize)

        return tracks
            .mapNotNull { track ->
                val title = track.title?.let(::normalize) ?: return@mapNotNull null
                val artistName = track.artist?.name?.let(::normalize) ?: return@mapNotNull null
                if (title.isBlank() || artistName.isBlank()) return@mapNotNull null

                var score = 0
                if (title == targetSong) score += 4 else if (title.contains(targetSong)) score += 2
                if (artistName == targetArtist) score += 4 else if (artistName.contains(targetArtist)) score += 2
                if (targetAlbum != null) {
                    val collection = track.album?.title?.let(::normalize)
                    if (collection != null) {
                        if (collection == targetAlbum) score += 3
                        else if (collection.contains(targetAlbum)) score += 1
                    }
                }
                if (score >= 6) track to score else null
            }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun urlEncode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
        .trim()
}
