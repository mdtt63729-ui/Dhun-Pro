/*
 * ArchiveTune (2026)
 * © Rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.brahmkshatriya.echo.dhun.spotify

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import dev.brahmkshatriya.echo.dhun.spotify.models.SpotifyAccessToken

/**
 * Exchanges Spotify account cookies (`sp_dc`, optionally `sp_key`) for a
 * Web API access token, using the same endpoint the Spotify web player uses:
 *
 * `GET https://open.spotify.com/get_access_token?reason=transport&productType=web_player`
 *
 * The returned token is a bearer token valid for the Web API; its absolute
 * expiry (epoch ms) is carried in
 * [SpotifyAccessToken.accessTokenExpirationTimestampMs].
 */
object SpotifyAuth {
    private const val OPEN_BASE = "https://open.spotify.com"
    private const val GET_ACCESS_TOKEN_PATH = "get_access_token"

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
        }

    private val client =
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 20_000
            }
            expectSuccess = false
        }

    /**
     * Fetches a fresh access token for the given cookies.
     *
     * @param spDc the `sp_dc` session cookie value (required, non-blank).
     * @param spKey the `sp_key` cookie value (optional; sent only if not blank).
     * @return the parsed token, or a failed [Result] on HTTP / parse errors.
     */
    suspend fun fetchAccessToken(
        spDc: String,
        spKey: String,
    ): Result<SpotifyAccessToken> =
        runCatching {
            withContext(Dispatchers.IO) {
                val response: HttpResponse =
                    client.get("$OPEN_BASE/$GET_ACCESS_TOKEN_PATH") {
                        parameter("reason", "transport")
                        parameter("productType", "web_player")
                        contentType(ContentType.Application.Json)
                        header(HttpHeaders.UserAgent, USER_AGENT)
                        header(
                            HttpHeaders.Cookie,
                            buildString {
                                append("sp_dc=")
                                append(spDc)
                                if (spKey.isNotBlank()) {
                                    append("; sp_key=")
                                    append(spKey)
                                }
                            },
                        )
                    }
                if (!response.status.isSuccess()) {
                    throw Spotify.SpotifyException(
                        statusCode = response.status.value,
                        message = response.bodyAsText(),
                    )
                }
                json.decodeFromString(SpotifyAccessToken.serializer(), response.bodyAsText())
            }
        }
}
