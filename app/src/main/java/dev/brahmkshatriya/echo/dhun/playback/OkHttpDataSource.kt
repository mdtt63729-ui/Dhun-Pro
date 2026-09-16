/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import okhttp3.Call
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException

/**
 * Minimal okhttp-backed [DataSource] for media3.
 *
 * Drop-in replacement for `androidx.media3.datasource.okhttp.OkHttpDataSource`
 * from the `media3-datasource-okhttp` artifact, which is not part of this
 * project's dependency set. It supports the subset of behaviour the Dhun
 * playback pipeline relies on:
 *  - GET/POST/HEAD requests,
 *  - byte-range requests driven by [DataSpec.position]/[DataSpec.length],
 *  - per-request headers from [DataSpec.httpRequestHeaders],
 *  - skipping the requested position when a server ignores the Range header.
 *
 * The okhttp [Call.Factory] (including its interceptors) is supplied by the
 * caller so request rewriting (user-agent, proxy, ...) keeps working exactly
 * as it did with the upstream implementation.
 */
@OptIn(UnstableApi::class)
class OkHttpDataSource(
    private val callFactory: Call.Factory,
    private val defaultRequestProperties: Map<String, String> = emptyMap(),
) : BaseDataSource(true) {

    class Factory(
        private val callFactory: Call.Factory,
        private val defaultRequestProperties: Map<String, String> = emptyMap(),
    ) : DataSource.Factory {
        override fun createDataSource(): OkHttpDataSource =
            OkHttpDataSource(callFactory, defaultRequestProperties)
    }

    private val requestProperties = mutableMapOf<String, String>()
    private var dataSpec: DataSpec? = null
    private var response: Response? = null
    private var body: ResponseBody? = null
    private var opened = false
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()

    fun setRequestProperty(name: String, value: String) {
        requestProperties[name] = value
    }

    fun clearAllRequestProperties() {
        requestProperties.clear()
    }

    private fun buildRequest(dataSpec: DataSpec): Request {
        val requestBuilder = Request.Builder().url(dataSpec.uri.toString())

        for ((name, value) in defaultRequestProperties) {
            requestBuilder.header(name, value)
        }
        for ((name, value) in requestProperties) {
            requestBuilder.header(name, value)
        }
        for ((name, value) in dataSpec.httpRequestHeaders) {
            requestBuilder.header(name, value)
        }

        val position = dataSpec.position
        if (position != 0L || dataSpec.length != C.LENGTH_UNSET.toLong()) {
            val rangeEnd = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                ""
            } else {
                (position + dataSpec.length - 1).toString()
            }
            requestBuilder.header("Range", "bytes=$position-$rangeEnd")
        }

        when (dataSpec.httpMethod) {
            DataSpec.HTTP_METHOD_GET -> Unit
            DataSpec.HTTP_METHOD_HEAD -> requestBuilder.head()
            DataSpec.HTTP_METHOD_POST ->
                requestBuilder.post(
                    (dataSpec.httpBody ?: ByteArray(0)).toRequestBody(null)
                )
            else ->
                throw IOException("Unsupported HTTP method: ${dataSpec.httpMethod}")
        }

        return requestBuilder.build()
    }

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        transferInitializing(dataSpec)

        val request = buildRequest(dataSpec)
        val response = callFactory.newCall(request).execute()
        this.response = response

        if (!response.isSuccessful) {
            val code = response.code
            val message = response.message
            closeConnectionQuietly()
            throw IOException("HTTP $code $message for ${dataSpec.uri}".trim())
        }

        val body = response.body ?: run {
            closeConnectionQuietly()
            throw IOException("Empty response body for ${dataSpec.uri}")
        }
        this.body = body
        opened = true
        transferStarted(dataSpec)

        // Handle a server that ignored the Range header by skipping manually.
        val responseCode = response.code
        var skipBytes = 0L
        if (responseCode == 200 && dataSpec.position > 0) {
            skipBytes = dataSpec.position
        }

        if (skipBytes > 0) {
            val stream = body.byteStream()
            var remainingToSkip = skipBytes
            while (remainingToSkip > 0) {
                val skipped = stream.skip(remainingToSkip)
                if (skipped <= 0) {
                    closeConnectionQuietly()
                    throw IOException("Unable to skip to position ${dataSpec.position}")
                }
                remainingToSkip -= skipped
            }
        }

        bytesRemaining =
            if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                val contentLength = body.contentLength()
                if (contentLength == -1L) {
                    C.LENGTH_UNSET.toLong()
                } else {
                    contentLength - skipBytes
                }
            }

        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (readLength == 0) return 0

        val body = body ?: throw IOException("Attempt to read from a closed source")

        val currentRemaining = bytesRemaining
        val toRead =
            if (currentRemaining == C.LENGTH_UNSET.toLong()) {
                readLength
            } else {
                minOf(readLength.toLong(), currentRemaining).toInt()
            }
        if (toRead == 0) return C.RESULT_END_OF_INPUT

        val bytesRead = body.byteStream().read(buffer, offset, toRead)
        if (bytesRead == -1) {
            if (currentRemaining != C.LENGTH_UNSET.toLong() && currentRemaining > 0) {
                throw IOException("Unexpected end of stream for ${dataSpec?.uri}")
            }
            return C.RESULT_END_OF_INPUT
        }

        if (currentRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining = currentRemaining - bytesRead
        }
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = dataSpec?.uri

    override fun close() {
        try {
            if (opened) {
                opened = false
                transferEnded()
            }
        } finally {
            closeConnectionQuietly()
            dataSpec = null
        }
    }

    private fun closeConnectionQuietly() {
        runCatching { body?.close() }
        runCatching { response?.close() }
        body = null
        response = null
        bytesRemaining = C.LENGTH_UNSET.toLong()
    }
}
