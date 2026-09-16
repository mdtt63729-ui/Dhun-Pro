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
 * Token payload returned by Spotify's `get_access_token` endpoint
 * (`https://open.spotify.com/get_access_token`), exchanged for a valid
 * `sp_dc` (and optionally `sp_key`) cookie.
 *
 * JSON shape:
 * ```json
 * {
 *   "clientId": "d8a5ed958d274c2e8ee717e6a4b0971d",
 *   "accessToken": "BQ...",
 *   "accessTokenExpirationTimestampMs": 1767225600000,
 *   "accessTokenExpirationTimestamp": "2026-01-01T00:00:00Z",
 *   "isAnonymous": false
 * }
 * ```
 */
@Serializable
data class SpotifyAccessToken(
    @SerialName("clientId") val clientId: String? = null,
    @SerialName("accessToken") val accessToken: String = "",
    @SerialName("accessTokenExpirationTimestampMs") val accessTokenExpirationTimestampMs: Long = 0L,
    @SerialName("isAnonymous") val isAnonymous: Boolean = false,
)
