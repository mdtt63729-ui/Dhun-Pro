/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.ui.screens.settings

import android.content.Context
import dev.brahmkshatriya.echo.dhun.db.entities.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Minimal ListenBrainz scrobbler used by the playback service.
 *
 * Submits "playing now" and finished ("single") listens to the public
 * ListenBrainz API (https://api.listenbrainz.org) using the user's token.
 */
object ListenBrainzManager {

    private const val TAG = "ListenBrainzManager"
    private const val SUBMIT_ENDPOINT = "https://api.listenbrainz.org/1/submit-listens"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val json = Json { encodeDefaults = false }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Submits a "playing now" listen for [song].
     *
     * @return true when the API accepted the submission.
     */
    suspend fun submitPlayingNow(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
    ): Boolean {
        if (song == null || token.isBlank()) return false
        val body = buildSubmitBody(
            listenType = "playing_now",
            listenedAt = null,
            song = song,
        )
        return submit(token, body)
    }

    /**
     * Submits a finished listen ("single") for [song].
     *
     * @param startMs epoch milliseconds at which the track started playing.
     * @param endMs epoch milliseconds at which the track finished.
     * @return true when the API accepted the submission.
     */
    suspend fun submitFinished(
        context: Context,
        token: String,
        song: Song?,
        startMs: Long,
        endMs: Long,
    ): Boolean {
        if (song == null || token.isBlank()) return false
        val body = buildSubmitBody(
            listenType = "single",
            listenedAt = (startMs / 1000L).coerceAtLeast(0L),
            song = song,
        )
        return submit(token, body)
    }

    private fun buildSubmitBody(
        listenType: String,
        listenedAt: Long?,
        song: Song,
    ): String {
        val durationSeconds = song.song.duration.takeIf { it > 0 }
        val listen = buildJsonObject {
            listenedAt?.let { put("listened_at", it) }
            putJsonObject("track_metadata") {
                put("track_name", song.song.title)
                put("artist_name", song.artists.joinToString(", ") { it.name })
                song.album?.title ?: song.song.albumName?.let { put("release_name", it) }
                putJsonObject("additional_info") {
                    put("submission_client", "Dhun")
                    put("media_player", "Dhun")
                    if (durationSeconds != null) {
                        put("duration_ms", durationSeconds * 1000L)
                    }
                    song.song.thumbnailUrl?.let { put("artwork_url", it) }
                }
            }
        }
        val payload = buildJsonArray { add(listen) }
        return json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            buildJsonObject {
                put("listen_type", listenType)
                put("payload", payload)
            },
        )
    }

    private suspend fun submit(token: String, body: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(SUBMIT_ENDPOINT)
                    .header("Authorization", "Token $token")
                    .post(body.toRequestBody(jsonMediaType))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Timber.tag(TAG).w(
                            "submit failed: HTTP %d %s",
                            response.code,
                            response.message,
                        )
                    }
                    response.isSuccessful
                }
            }.getOrElse { t ->
                Timber.tag(TAG).w(t, "submit failed")
                false
            }
        }
}
