/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.jossredconnect

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * In-source client for the JossRed multimedia backend.
 *
 * JossRed acts as an alternative stream resolver: when the primary YouTube
 * playback pipeline fails, Dhun asks JossRed for a direct streaming URL for
 * the media id and falls back to it. The upstream `jossredconnect` library is
 * unpublished, so this class re-implements the small API surface used by
 * `MusicService`:
 *
 * ```kotlin
 * val url = JossRedClient.getStreamingUrl(mediaId) // suspend, nullable
 * ```
 *
 * Failures (network, HTTP, unexpected payload) are reported by throwing
 * [JossRedException]; a missing URL yields `null`.
 */
object JossRedClient {

    /**
     * Base URL of the JossRed backend. Configurable so builds can point the
     * client at a self-hosted instance.
     */
    var baseUrl: String = "https://jossred.app"

    class JossRedException(
        message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves a direct streaming URL for [mediaId].
     *
     * @param mediaId the media id (usually a YouTube video id).
     * @return the streaming URL, or null when the backend has no stream.
     * @throws JossRedException on network / HTTP / protocol failures.
     */
    suspend fun getStreamingUrl(mediaId: String): String? {
        if (mediaId.isBlank()) return null
        return withContext(Dispatchers.IO) {
            val request = runCatching {
                Request.Builder()
                    .url("$baseUrl/api/v1/stream?videoId=$mediaId")
                    .header("Accept", "application/json")
                    .build()
            }.getOrElse { throw JossRedException("Invalid JossRed request for $mediaId", it) }

            val body = runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw JossRedException(
                            "JossRed returned HTTP ${response.code} for $mediaId"
                        )
                    }
                    response.body?.string()
                }
            }.getOrElse { t ->
                if (t is JossRedException) throw t
                throw JossRedException("JossRed request failed for $mediaId", t)
            }

            if (body.isNullOrBlank()) {
                throw JossRedException("JossRed returned an empty body for $mediaId")
            }

            runCatching {
                val root = json.parseToJsonElement(body).jsonObject
                // The backend returns the direct URL under one of a few common
                // keys; accept the first that is present and non-blank.
                for (key in listOf("url", "audioUrl", "streamUrl", "stream_url")) {
                    val value = root[key]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
                    if (!value.isNullOrBlank()) return@runCatching value
                }
                null
            }.getOrElse { t ->
                throw JossRedException("Malformed JossRed response for $mediaId", t)
            }
        }
    }
}
