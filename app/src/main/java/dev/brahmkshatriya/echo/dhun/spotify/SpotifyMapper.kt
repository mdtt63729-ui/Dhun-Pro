/*
 * ArchiveTune (2026)
 * © Rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.brahmkshatriya.echo.dhun.spotify

import dev.brahmkshatriya.echo.dhun.models.MediaMetadata
import dev.brahmkshatriya.echo.dhun.spotify.models.SpotifyPlaylist
import dev.brahmkshatriya.echo.dhun.spotify.models.SpotifyTrack
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Maps Spotify models onto this project's domain models and builds the
 * fuzzy-matching primitives used by [SpotifyPlaybackResolver] to resolve a
 * [SpotifyTrack] to a playable item (the project plays through YouTube).
 */
object SpotifyMapper {
    private const val DURATION_TOLERANCE_SEC = 30.0

    /**
     * Normalized, reusable representation of one side of a track comparison.
     * Computed once per Spotify track via [precompute], then compared against
     * many candidates via [matchScorePrecomputed].
     */
    data class PrecomputedTrack(
        val normalizedTitle: String,
        val titleTokens: Set<String>,
        val artistTokens: Set<String>,
        val durationSec: Int,
    )

    /**
     * Builds the search query used to look up a Spotify track on YouTube:
     * `"<artists> <title> [<album name>]"`, falling back to just the title.
     */
    fun buildSearchQuery(track: SpotifyTrack): String {
        val artists =
            track.artists
                .map { it.name.trim() }
                .filter(String::isNotBlank)
                .joinToString(" ")
        val album = track.album?.name?.trim().orEmpty()
        return buildString {
            if (artists.isNotEmpty()) {
                append(artists)
                append(' ')
            }
            append(track.name.trim())
            if (album.isNotEmpty()) {
                append(' ')
                append(album)
            }
        }.trim()
    }

    /** Precomputes the normalized/matching representation of a track. */
    fun precompute(
        title: String,
        artist: String,
        durationMs: Int,
    ): PrecomputedTrack =
        PrecomputedTrack(
            normalizedTitle = normalize(title),
            titleTokens = tokenize(title),
            artistTokens = tokenize(artist),
            durationSec = if (durationMs > 0) durationMs / 1000 else 0,
        )

    /** Convenience overload matching [matchScorePrecomputed] for a full track. */
    fun matchScore(
        track: SpotifyTrack,
        candidateTitle: String,
        candidateArtist: String,
        candidateDurationSec: Int?,
    ): Double {
        val precomputed =
            precompute(
                title = track.name,
                artist = track.artists.joinToString(" ") { it.name },
                durationMs = track.durationMs,
            )
        return matchScorePrecomputed(
            precomputed = precomputed,
            candidateTitle = candidateTitle,
            candidateArtist = candidateArtist,
            candidateDurationSec = candidateDurationSec,
        )
    }

    /**
     * Scores how well a candidate (a YouTube search result) matches a
     * precomputed Spotify track. The score is in `[0.0, 1.0]`; 1.0 is a
     * perfect match. `SpotifyPlaybackResolver` accepts candidates scoring at
     * least `0.35`.
     *
     * Weights: title edit distance 0.45, title token overlap 0.15,
     * artist token overlap 0.25, duration closeness 0.15.
     */
    fun matchScorePrecomputed(
        precomputed: PrecomputedTrack,
        candidateTitle: String,
        candidateArtist: String,
        candidateDurationSec: Int?,
    ): Double {
        val titleScore = similarity(precomputed.normalizedTitle, normalize(candidateTitle))
        val titleTokenScore = tokenOverlap(precomputed.titleTokens, tokenize(candidateTitle))
        val artistScore = tokenOverlap(precomputed.artistTokens, tokenize(candidateArtist))
        val durationScore =
            when {
                precomputed.durationSec <= 0 -> 0.5
                candidateDurationSec == null -> 0.5
                else -> durationCloseness(precomputed.durationSec, candidateDurationSec)
            }
        val score =
            0.45 * titleScore +
                0.15 * titleTokenScore +
                0.25 * artistScore +
                0.15 * durationScore
        return score.coerceIn(0.0, 1.0)
    }

    /** Best artwork URL for a track (Spotify exposes it on the album object). */
    fun getTrackThumbnail(track: SpotifyTrack): String? =
        track.album
            ?.images
            .orEmpty()
            .firstOrNull { it.url.isNotBlank() }
            ?.url
            ?.takeIf(String::isNotBlank)

    /** Best artwork URL for a playlist. */
    fun getPlaylistThumbnail(playlist: SpotifyPlaylist): String? =
        playlist.images
            .firstOrNull { it.url.isNotBlank() }
            ?.url
            ?.takeIf(String::isNotBlank)

    /**
     * Maps a [SpotifyTrack] into the project's playable song model
     * ([MediaMetadata] — the same model used by the player, the queue and
     * `MediaMetadata.toMediaItem()`), tagging it with the Spotify track id in
     * [MediaMetadata.spotifyTrackId].
     *
     * Playback of a raw Spotify track is resolved by [SpotifyPlaybackResolver],
     * which finds the best YouTube match and keeps the `spotifyTrackId`.
     */
    fun toMediaMetadata(track: SpotifyTrack): MediaMetadata =
        MediaMetadata(
            id = track.id,
            title = track.name,
            artists =
                track.artists.map {
                    MediaMetadata.Artist(
                        id = it.id,
                        name = it.name,
                        thumbnailUrl = null,
                    )
                },
            duration =
                if (track.durationMs > 0) {
                    track.durationMs / 1000
                } else {
                    MediaMetadata.UNKNOWN_DURATION
                },
            thumbnailUrl = getTrackThumbnail(track),
            album =
                track.album?.let {
                    MediaMetadata.Album(
                        id = it.id,
                        title = it.name,
                    )
                },
            explicit = track.explicit,
            spotifyTrackId = track.id.takeIf(String::isNotBlank),
        )

    private val noiseTokens =
        setOf(
            "the", "a", "an", "and", "or", "of", "feat", "ft", "featuring",
            "with", "from", "by", "official", "video", "audio", "lyrics",
            "lyric", "remastered", "remaster", "version", "single", "ep",
            "album", "music", "song", "hd", "hq", "vevo",
        )

    /**
     * Normalizes a title/artist for comparison: lowercase, drop bracketed
     * qualifiers such as `(feat. X)` / `[Remastered]` / `- Single`, strip
     * punctuation and collapse whitespace.
     */
    private fun normalize(value: String): String {
        var cleaned = value
        cleaned =
            cleaned.replace(
                Regex("""\((feat|ft|featuring|with|prod|remaster)[^)]*\)""", RegexOption.IGNORE_CASE),
                " ",
            )
        cleaned =
            cleaned.replace(
                Regex("""\[(feat|ft|featuring|with|prod|remaster)[^\]]*\]""", RegexOption.IGNORE_CASE),
                " ",
            )
        cleaned =
            cleaned.replace(
                Regex("""[-–]\s*(single|ep|remaster(ed)?|live|deluxe|version)[^a-zA-Z0-9]*$""", RegexOption.IGNORE_CASE),
                " ",
            )
        cleaned = cleaned.lowercase().replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
        return cleaned.split(Regex("""\s+""")).filter(String::isNotBlank).joinToString(" ")
    }

    /** Splits a normalized string into a set of meaningful tokens. */
    private fun tokenize(value: String): Set<String> =
        normalize(value)
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() && it !in noiseTokens }
            .toSet()

    /** Normalized Levenshtein similarity in `[0.0, 1.0]`. */
    private fun similarity(
        a: String,
        b: String,
    ): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        val maxLen = max(a.length, b.length)
        return 1.0 - levenshtein(a, b).toDouble() / maxLen
    }

    private fun levenshtein(
        a: String,
        b: String,
    ): Int {
        val lhs = a.length
        val rhs = b.length
        if (lhs == 0) return rhs
        if (rhs == 0) return lhs
        var previous = IntArray(lhs + 1) { it }
        var current = IntArray(lhs + 1)
        for (j in 1..rhs) {
            current[0] = j
            for (i in 1..lhs) {
                val substitutionCost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[i] =
                    min(
                        previous[i] + 1,
                        min(
                            current[i - 1] + 1,
                            previous[i - 1] + substitutionCost,
                        ),
                    )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[lhs]
    }

    /**
     * Token overlap as an F1 score over the two token sets.
     * Returns 1.0 when both sets are empty, 0.0 when exactly one is empty.
     */
    private fun tokenOverlap(
        a: Set<String>,
        b: Set<String>,
    ): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val common = a.intersect(b).size
        if (common == 0) return 0.0
        val precision = common.toDouble() / b.size
        val recall = common.toDouble() / a.size
        return 2.0 * precision * recall / (precision + recall)
    }

    /** Duration agreement in `[0.0, 1.0]`; 0.0 beyond [DURATION_TOLERANCE_SEC]. */
    private fun durationCloseness(
        expectedSec: Int,
        actualSec: Int,
    ): Double {
        val diff = abs(expectedSec - actualSec).toDouble()
        return (1.0 - diff / DURATION_TOLERANCE_SEC).coerceIn(0.0, 1.0)
    }
}
