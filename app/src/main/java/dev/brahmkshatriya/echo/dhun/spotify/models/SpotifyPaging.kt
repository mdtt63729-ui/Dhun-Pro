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
 * A generic Spotify paging object (cursorless offset-based paging).
 *
 * https://developer.spotify.com/documentation/web-api/reference/get-playlist-tracks
 *
 * JSON shape:
 * ```json
 * {
 *   "href": "https://api.spotify.com/v1/...?offset=0&limit=50",
 *   "items": [ ... ],
 *   "limit": 50,
 *   "next": "https://api.spotify.com/v1/...?offset=50&limit=50",
 *   "offset": 0,
 *   "previous": null,
 *   "total": 120
 * }
 * ```
 */
@Serializable
data class SpotifyPaging<T>(
    val href: String? = null,
    val items: List<T> = emptyList(),
    val limit: Int = 0,
    val next: String? = null,
    val offset: Int = 0,
    val previous: String? = null,
    val total: Int = 0,
)

/**
 * An entry of the `GET /playlists/{id}/tracks` paging response.
 *
 * JSON shape:
 * ```json
 * {
 *   "added_at": "2024-01-01T00:00:00Z",
 *   "added_by": { "id": "...", "display_name": "..." },
 *   "is_local": false,
 *   "track": { "id": "...", "name": "...", "duration_ms": 213000, "artists": [...] }
 * }
 * ```
 *
 * `track` is null for removed/unavailable entries, and the endpoint can also
 * return podcast episodes in the same wrapper (which still decode into
 * [SpotifyTrack] because of [kotlinx.serialization.json.Json]'s
 * `ignoreUnknownKeys` and the model defaults).
 */
@Serializable
data class SpotifyPlaylistTrack(
    @SerialName("added_at") val addedAt: String? = null,
    @SerialName("added_by") val addedBy: SpotifyUser? = null,
    @SerialName("is_local") val isLocal: Boolean = false,
    val track: SpotifyTrack? = null,
)
