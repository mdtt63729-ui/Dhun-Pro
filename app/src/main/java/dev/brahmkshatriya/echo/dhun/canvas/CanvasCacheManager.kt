/*
 * Dhun Project (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.canvas

import android.content.Context
import android.util.LruCache
import dev.brahmkshatriya.echo.dhun.canvas.models.CanvasArtwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Shared, module-internal HTTP helpers for the canvas providers.
 *
 * A single client with conservative timeouts is used for every provider call
 * (search + URL verification), so a hanging provider cannot stall the resolver.
 */
internal object CanvasHttp {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Executes [block] with the shared client and returns `null` on any failure
     * (network error, non-2xx status). Never throws.
     */
    fun <T> runCatchingRequest(tag: String, block: (OkHttpClient) -> T): T? = try {
        block(client)
    } catch (e: Exception) {
        Timber.w(e, "$tag request failed")
        null
    }

    /**
     * Verifies that [url] actually serves media before it is surfaced to the
     * player. Issues a small ranged GET (some CDNs reject HEAD) and accepts the
     * URL when the response is 2xx/206 and the content type is either missing or
     * looks like media (video/…, application/vnd.apple.mpegurl,
     * application/x-mpegurl, application/octet-stream, or text/plain for m3u8
     * playlists).
     */
    suspend fun verifyMediaUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatchingRequest("CanvasHttp.verify") { c ->
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
                .get()
                .build()
            c.newCall(request).execute().use { response ->
                val code = response.code
                if (code !in 200..299) return@runCatchingRequest false
                val contentType = response.header("Content-Type")
                    ?.lowercase(Locale.ROOT)
                    .orEmpty()
                contentType.isEmpty() ||
                    contentType.startsWith("video/") ||
                    contentType.startsWith("audio/") ||
                    contentType.contains("mpegurl") ||
                    contentType.contains("octet-stream") ||
                    (contentType.startsWith("text/plain") &&
                        url.contains(".m3u8", ignoreCase = true))
            }
        } ?: false
    }
}

/**
 * Caches canvas/animated artwork for the player UI:
 *  - a memory cache of [CanvasArtwork] keyed by song id *and* by animation URL, and
 *  - a disk cache of downloaded animation files (under `cacheDir/canvas`), keyed
 *    by a SHA-256 of the URL.
 *
 * The disk cache uses plain `java.io.File` storage under the app's
 * [Context.getCacheDir] (so Android can reclaim it when storage is low) with a
 * size cap and least-recently-used eviction based on file timestamps.
 *
 * The public surface required by existing call sites:
 *  - [init] — called from `MainActivity.onStart()` as `CanvasCacheManager.init(this)`.
 *  - [getCachedCanvasByUrl] — called from `CanvasArtworkPlayer` inside a
 *    `LaunchedEffect`; returns a hit only when the animation was already
 *    downloaded to disk.
 */
object CanvasCacheManager {

    /**
     * A disk-cache hit for a canvas animation.
     *
     * @param url       The remote URL the file was cached for.
     * @param filePath  Absolute path of the local file; never null on a hit.
     * @param sizeBytes Size of the cached file in bytes.
     * @param cachedAt  Epoch millis of when the file was last touched.
     */
    data class CachedCanvas(
        val url: String,
        val filePath: String,
        val sizeBytes: Long,
        val cachedAt: Long,
    )

    private const val TAG = "CanvasCacheManager"
    private const val DIR_NAME = "canvas"
    private const val DEFAULT_MAX_DISK_CACHE_BYTES: Long = 256L * 1024 * 1024 // 256 MB

    /** Background scope used for best-effort eviction sweeps. */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Memory entries are keyed by song id AND by each animation URL. */
    private val memoryCache = LruCache<String, CanvasArtwork>(1024)

    private val diskMutex = Mutex()

    @Volatile
    private var diskCacheDir: File? = null

    @Volatile
    private var maxDiskCacheBytes: Long = DEFAULT_MAX_DISK_CACHE_BYTES

    /** `true` once [init] has been called with a [Context]. */
    val isInitialized: Boolean
        get() = diskCacheDir != null

    /**
     * Initializes the cache directory (under the app's `cacheDir`) and schedules
     * a trim to the size cap. Safe to call repeatedly (e.g. from every
     * `onStart()`); a second call with the same directory is a no-op.
     */
    fun init(context: Context) {
        val dir = File(context.cacheDir, DIR_NAME)
        if (!dir.exists() && !dir.mkdirs()) {
            Timber.w("$TAG: could not create disk cache dir ${dir.absolutePath}")
        }
        val previous = diskCacheDir
        diskCacheDir = dir
        if (previous?.absolutePath != dir.absolutePath) {
            scheduleTrim()
        }
    }

    // ------------------------------------------------------------------
    // Memory cache
    // ------------------------------------------------------------------

    /**
     * Stores [artwork] in memory under [songId] and indexes it under its
     * animation URLs as well, so it can also be found by URL only.
     */
    fun cacheArtwork(songId: String, artwork: CanvasArtwork) {
        if (songId.isBlank()) return
        synchronized(memoryCache) {
            memoryCache.put(songKey(songId), artwork)
            artwork.animated?.takeIf { it.isNotBlank() }?.let {
                memoryCache.put(urlKey(it), artwork)
            }
            artwork.videoUrl?.takeIf { it.isNotBlank() }?.let {
                memoryCache.put(urlKey(it), artwork)
            }
        }
    }

    /** Returns the artwork cached for [songId], or `null`. */
    fun getArtwork(songId: String): CanvasArtwork? = synchronized(memoryCache) {
        memoryCache.get(songKey(songId))
    }

    /** Returns the artwork cached for animation URL [url], or `null`. */
    fun getArtworkForUrl(url: String): CanvasArtwork? = synchronized(memoryCache) {
        memoryCache.get(urlKey(url))
    }

    /** Clears the in-memory artwork cache. */
    fun clearMemoryCache() {
        synchronized(memoryCache) { memoryCache.evictAll() }
    }

    // ------------------------------------------------------------------
    // Disk cache
    // ------------------------------------------------------------------

    /**
     * Returns a disk-cache hit for [url] when the animation has already been
     * downloaded locally, `null` otherwise. Does NOT download; use
     * [downloadCanvas] to prefetch.
     */
    suspend fun getCachedCanvasByUrl(url: String): CachedCanvas? = withContext(Dispatchers.IO) {
        val file = cachedFileFor(url) ?: return@withContext null
        if (!file.isFile || file.length() <= 0L) return@withContext null
        // Touch for LRU purposes.
        runCatching { file.setLastModified(System.currentTimeMillis()) }
        CachedCanvas(
            url = url,
            filePath = file.absolutePath,
            sizeBytes = file.length(),
            cachedAt = file.lastModified(),
        )
    }

    /**
     * Downloads [url] into the disk cache and returns the resulting hit, or
     * `null` on failure. When [songId] is provided and the URL is present in
     * the memory cache, the artwork is also indexed under that song id.
     */
    suspend fun downloadCanvas(url: String, songId: String? = null): CachedCanvas? =
        withContext(Dispatchers.IO) {
            val dir = diskCacheDir ?: return@withContext null
            if (url.isBlank()) return@withContext null

            // Fast path: already cached.
            getCachedCanvasByUrl(url)?.let { return@withContext it }

            val hit = diskMutex.withLock {
                val target = File(dir, fileNameFor(url))
                val temp = File(dir, "${target.name}.tmp")
                val ok = CanvasHttp.runCatchingRequest(TAG) { c ->
                    c.newCall(Request.Builder().url(url).get().build())
                        .execute()
                        .use { response ->
                            if (!response.isSuccessful) return@runCatchingRequest false
                            val body = response.body ?: return@runCatchingRequest false
                            temp.outputStream().use { out ->
                                body.byteStream().copyTo(out)
                            }
                            true
                        }
                } == true

                if (!ok) {
                    runCatching { temp.delete() }
                    Timber.w("$TAG: failed to download canvas $url")
                    return@withLock null
                }

                if (target.exists()) runCatching { target.delete() }
                if (!temp.renameTo(target)) {
                    runCatching { temp.delete() }
                    Timber.w("$TAG: could not move cache file for $url")
                    return@withLock null
                }

                trimDiskCacheNoLock()

                CachedCanvas(
                    url = url,
                    filePath = target.absolutePath,
                    sizeBytes = target.length(),
                    cachedAt = target.lastModified(),
                )
            }

            if (hit != null && songId != null) {
                getArtworkForUrl(url)?.let { cacheArtwork(songId, it) }
            }
            hit
        }

    /**
     * Returns the local [File] a given [url] maps to, or `null` when the
     * manager has not been initialized. Does not check existence.
     */
    fun cachedFileFor(url: String): File? {
        val dir = diskCacheDir ?: return null
        return File(dir, fileNameFor(url))
    }

    /**
     * Deletes every downloaded canvas from disk (and clears the memory cache
     * too, since its URL entries would otherwise dangle).
     */
    suspend fun clearDiskCache() = withContext(Dispatchers.IO) {
        diskMutex.withLock {
            val dir = diskCacheDir ?: return@withLock
            dir.listFiles()?.forEach { file ->
                runCatching { file.delete() }
            }
        }
        clearMemoryCache()
    }

    /** Current size of the disk cache in bytes (0 when uninitialized). */
    fun diskCacheSizeBytes(): Long {
        val dir = diskCacheDir ?: return 0L
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Adjusts the disk cache cap, in bytes. Pass 0 to effectively disable disk
     * caching (the next eviction sweep removes existing files).
     */
    fun setMaxDiskCacheBytes(bytes: Long) {
        maxDiskCacheBytes = bytes.coerceAtLeast(0L)
        scheduleTrim()
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun scheduleTrim() {
        ioScope.launch { trimDiskCache() }
    }

    /** Serializes via [diskMutex]. */
    private suspend fun trimDiskCache() = withContext(Dispatchers.IO) {
        diskMutex.withLock { trimDiskCacheNoLock() }
    }

    /** Caller must already hold [diskMutex]. */
    private fun trimDiskCacheNoLock() {
        val dir = diskCacheDir ?: return
        val cap = maxDiskCacheBytes
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= cap) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= cap) break
            val size = file.length()
            if (runCatching { file.delete() }.getOrDefault(false)) {
                total -= size
            }
        }
    }

    private fun songKey(songId: String) = "song:${songId.trim()}"

    private fun urlKey(url: String) = "url:${url.trim()}"

    private fun fileNameFor(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(url.trim().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val ext = when {
            url.contains(".m3u8", ignoreCase = true) -> ".m3u8"
            url.contains(".mp4", ignoreCase = true) -> ".mp4"
            url.contains(".webm", ignoreCase = true) -> ".webm"
            url.contains(".mpd", ignoreCase = true) -> ".mpd"
            else -> ".bin"
        }
        return "$digest$ext"
    }
}
