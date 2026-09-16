/*
 * ArchiveTune (2026)
 * © Rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.brahmkshatriya.echo.dhun.spotify

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import dev.brahmkshatriya.echo.dhun.spotify.models.SpotifyPaging
import dev.brahmkshatriya.echo.dhun.spotify.models.SpotifyPlaylist
import dev.brahmkshatriya.echo.dhun.spotify.models.SpotifyPlaylistTrack
import dev.brahmkshatriya.echo.dhun.spotify.models.SpotifyUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Minimal Spotify Web API client.
 *
 * The bearer access token is kept in [accessToken] and is populated by
 * [SpotifyAuth] / `SpotifyLibraryRepository`. Every call is wrapped in
 * [Result] so callers can use `getOrThrow` / `onSuccess` / `onFailure`.
 */
object Spotify {
    private const val API_BASE = "https://api.spotify.com/v1"

    /** Bearer token used for all Web API requests. `null` when logged out. */
    @Volatile
    var accessToken: String? = null

    /**
     * Thrown when the Spotify Web API answers with a non-2xx status code.
     *
     * [statusCode] is the HTTP status; `401` is treated by
     * `SpotifyLibraryRepository.spotifyCallWithTokenRetry` as
     * "token expired, refresh and retry".
     */
    class SpotifyException(
        val statusCode: Int,
        message: String?,
    ) : Exception(message ?: "Spotify request failed with status code $statusCode")

    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
            isLenient = true
        }

    private val client =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 20_000
            }
            expectSuccess = false
        }

    /** `GET /me` — the current user's profile. */
    suspend fun me(): Result<SpotifyUser> = request("me") { it.body<SpotifyUser>() }

    /** `GET /playlists/{id}` — a single playlist with its `tracks.total` reference. */
    suspend fun playlist(playlistId: String): Result<SpotifyPlaylist> =
        request("playlists/$playlistId") { it.body<SpotifyPlaylist>() }

    /** `GET /me/playlists` — one page of the current user's playlists. */
    suspend fun myPlaylists(
        limit: Int = 50,
        offset: Int = 0,
    ): Result<SpotifyPaging<SpotifyPlaylist>> =
        request("me/playlists", limit = limit, offset = offset) {
            it.body<SpotifyPaging<SpotifyPlaylist>>()
        }

    /** `GET /playlists/{id}/tracks` — one page of a playlist's tracks. */
    suspend fun playlistTracks(
        playlistId: String,
        limit: Int = 50,
        offset: Int = 0,
    ): Result<SpotifyPaging<SpotifyPlaylistTrack>> =
        request("playlists/$playlistId/tracks", limit = limit, offset = offset) {
            it.body<SpotifyPaging<SpotifyPlaylistTrack>>()
        }

    private suspend fun <T> request(
        path: String,
        limit: Int? = null,
        offset: Int? = null,
        block: suspend (HttpResponse) -> T,
    ): Result<T> =
        runCatching {
            withContext(Dispatchers.IO) {
                val token =
                    accessToken
                        ?: throw SpotifyException(
                            statusCode = HttpStatusCode.Unauthorized.value,
                            message = "Spotify is not connected",
                        )
                val response =
                    client.get("$API_BASE/$path") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        contentType(ContentType.Application.Json)
                        limit?.let { parameter("limit", it) }
                        offset?.let { parameter("offset", it) }
                    }
                if (!response.status.isSuccess()) {
                    throw SpotifyException(
                        statusCode = response.status.value,
                        message = response.bodyAsText(),
                    )
                }
                block(response)
            }
        }
}
