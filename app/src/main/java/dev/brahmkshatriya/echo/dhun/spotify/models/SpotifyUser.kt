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
 * An image object as returned by the Spotify Web API.
 *
 * https://developer.spotify.com/documentation/web-api/reference/get-track
 */
@Serializable
data class SpotifyImage(
    val url: String = "",
    val height: Int? = null,
    val width: Int? = null,
)

/**
 * A Spotify user / profile (used for `GET /me` and playlist owners).
 *
 * JSON shape:
 * ```json
 * {
 *   "id": "wizzler",
 *   "display_name": "Wizzler",
 *   "images": [ { "url": "...", "height": null, "width": null } ]
 * }
 * ```
 */
@Serializable
data class SpotifyUser(
    val id: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val images: List<SpotifyImage> = emptyList(),
    val email: String? = null,
    val country: String? = null,
    val product: String? = null,
)
