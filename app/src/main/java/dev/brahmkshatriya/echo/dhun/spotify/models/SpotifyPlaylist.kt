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
 * The `tracks` reference embedded in a Spotify playlist object.
 *
 * JSON shape:
 * ```json
 * { "href": "https://api.spotify.com/v1/playlists/.../tracks", "total": 42 }
 * ```
 */
@Serializable
data class SpotifyPlaylistTracksRef(
    val href: String? = null,
    val total: Int = 0,
)

/**
 * The owner of a Spotify playlist (simplified user object).
 */
@Serializable
data class SpotifyOwner(
    val id: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val uri: String? = null,
)

/**
 * A Spotify playlist as returned by the Spotify Web API.
 *
 * JSON shape (fields relevant to this project):
 * ```json
 * {
 *   "id": "37i9dQZF1DXcBWIGoYBM5M",
 *   "name": "Today's Top Hits",
 *   "uri": "spotify:playlist:37i9dQZF1DXcBWIGoYBM5M",
 *   "description": "...",
 *   "images": [ { "url": "...", "height": null, "width": null } ],
 *   "tracks": { "href": "...", "total": 50 },
 *   "owner": { "id": "...", "display_name": "..." }
 * }
 * ```
 */
@Serializable
data class SpotifyPlaylist(
    val id: String = "",
    val name: String = "",
    val uri: String? = null,
    val description: String? = null,
    val images: List<SpotifyImage> = emptyList(),
    val tracks: SpotifyPlaylistTracksRef? = null,
    val owner: SpotifyOwner? = null,
    val collaborative: Boolean? = null,
    @SerialName("public") val isPublic: Boolean? = null,
    @SerialName("snapshot_id") val snapshotId: String? = null,
)
