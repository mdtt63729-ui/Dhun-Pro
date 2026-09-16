/*
 * ArchiveTune (2026)
 * © Rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.brahmkshatriya.echo.dhun.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A Spotify artist as returned by the Spotify Web API.
 *
 * https://developer.spotify.com/documentation/web-api/reference/get-track
 */
@Serializable
data class SpotifyArtist(
    val id: String? = null,
    val name: String = "",
    val uri: String? = null,
)

/**
 * A Spotify album (simplified) as returned by the Spotify Web API.
 */
@Serializable
data class SpotifyAlbum(
    val id: String = "",
    val name: String = "",
    val uri: String? = null,
    @SerialName("album_type") val albumType: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("total_tracks") val totalTracks: Int? = null,
    val images: List<SpotifyImage> = emptyList(),
)

/**
 * A Spotify track as returned by the Spotify Web API.
 *
 * JSON shape (fields relevant to this project):
 * ```json
 * {
 *   "id": "4vuX4vJlCJBqrlFCh7bz8I",
 *   "name": "Song name",
 *   "uri": "spotify:track:4vuX4vJlCJBqrlFCh7bz8I",
 *   "duration_ms": 213000,
 *   "explicit": false,
 *   "is_local": false,
 *   "artists": [ { "id": "...", "name": "...", "uri": "..." } ],
 *   "album": { "id": "...", "name": "...", "images": [ { "url": "...", "height": 640, "width": 640 } ] }
 * }
 * ```
 */
@Serializable
data class SpotifyTrack(
    val id: String = "",
    val name: String = "",
    val uri: String? = null,
    @SerialName("duration_ms") val durationMs: Int = 0,
    val explicit: Boolean = false,
    @SerialName("is_local") val isLocal: Boolean = false,
    val artists: List<SpotifyArtist> = emptyList(),
    val album: SpotifyAlbum? = null,
    @SerialName("is_playable") val isPlayable: Boolean? = null,
    @SerialName("preview_url") val previewUrl: String? = null,
    @SerialName("track_number") val trackNumber: Int? = null,
    val popularity: Int? = null,
)
