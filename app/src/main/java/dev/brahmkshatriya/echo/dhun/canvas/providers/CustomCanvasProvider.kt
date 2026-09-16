/*
 * Dhun Project (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.canvas.providers

import android.content.Context
import dev.brahmkshatriya.echo.dhun.canvas.models.CanvasArtwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.Locale

/**
 * Resolves user-defined ("custom") canvas artwork for a song.
 *
 * This provider does not talk to any network service. Instead it keeps a
 * registry of user mappings `song + artist (+ album) -> animation URLs`:
 *
 *  - programmatically through [setCustomCanvas] / [removeCustomCanvas] /
 *    [clear];
 *  - persisted as JSON in `filesDir/custom_canvas.json` (auto-loaded on
 *    [init], auto-saved with a small debounce after every mutation);
 *  - or bulk-imported from a JSON document via [importJson] (the same schema
 *    as [exportJson] produces).
 *
 * Lookup normalizes titles/artists (case-insensitive, whitespace-collapsed)
 * and accepts a match when the registered song title equals the queried one
 * and the artist matches (equality, or one contains the other). A registered
 * album refines the match but is never required.
 *
 * While [init] has not been called, the provider works in memory-only mode
 * (mutations are kept for the process lifetime but not persisted).
 */
object CustomCanvasProvider {

    private const val TAG = "CustomCanvasProvider"
    private const val PERSIST_FILE = "custom_canvas.json"
    private const val PERSIST_DEBOUNCE_MS = 1_000L

    /** A single user-registered canvas mapping. */
    @Serializable
    data class CustomCanvasEntry(
        val song: String,
        val artist: String,
        val album: String? = null,
        val animatedUrl: String? = null,
        val videoUrl: String? = null,
        val staticUrl: String? = null,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        explicitNulls = false
    }

    private val listSerializer = ListSerializer(CustomCanvasEntry.serializer())

    @Volatile
    private var persistFile: File? = null

    private var loaded = false

    private val entries = mutableListOf<CustomCanvasEntry>()
    private val mutex = Mutex()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var persistJob: Job? = null

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Enables persistence: loads `filesDir/custom_canvas.json` (once) into the
     * in-memory registry. Safe to call on every app start.
     */
    fun init(context: Context) {
        val file = File(context.filesDir, PERSIST_FILE)
        val previous = persistFile
        persistFile = file
        if (previous?.absolutePath == file.absolutePath) return
        ioScope.launch { loadLocked() }
    }

    /**
     * Resolves the custom canvas registered for `song` by `artist`.
     *
     * @param song   Song title.
     * @param artist Artist name.
     * @param album  Album name, when known — used to prefer an exact album
     *               match over a generic one. Empty/blank means "any album".
     * @return a [CanvasArtwork] built from the registered entry (only when it
     *         carries at least one animation URL), or `null`.
     */
    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String = "",
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        if (song.isBlank() || artist.isBlank()) return@withContext null
        ensureLoaded()

        val targetSong = normalize(song)
        val targetArtist = normalize(artist)
        val targetAlbum = album.takeIf { it.isNotBlank() }?.let(::normalize)

        val matches = mutex.withLock {
            entries.filter { entry ->
                val entrySong = normalize(entry.song)
                val entryArtist = normalize(entry.artist)
                if (entrySong.isBlank() || entryArtist.isBlank()) return@filter false
                val songMatch = entrySong == targetSong
                val artistMatch = entryArtist == targetArtist ||
                    entryArtist.contains(targetArtist) ||
                    targetArtist.contains(entryArtist)
                songMatch && artistMatch
            }.sortedByDescending { entry ->
                // Prefer entries that also match the album.
                if (targetAlbum != null && entry.album != null &&
                    normalize(entry.album) == targetAlbum
                ) 1 else 0
            }
        }

        val best = matches.firstOrNull { entry ->
            !entry.animatedUrl.isNullOrBlank() || !entry.videoUrl.isNullOrBlank()
        } ?: return@withContext null

        CanvasArtwork(
            songTitle = entryField(best.song, song),
            artist = entryField(best.artist, artist),
            album = best.album ?: album.takeIf { it.isNotBlank() },
            animated = best.animatedUrl?.takeIf { it.isNotBlank() },
            videoUrl = best.videoUrl?.takeIf { it.isNotBlank() },
            staticUrl = best.staticUrl?.takeIf { it.isNotBlank() },
            provider = CanvasArtwork.PROVIDER_CUSTOM,
            type = CanvasArtwork.TYPE_VIDEO,
        )
    }

    /**
     * Registers (or replaces, when one exists for the same normalized
     * song/artist/album) a custom canvas.
     *
     * @return `true` when a new entry was added, `false` when an existing one
     *         was updated.
     */
    fun setCustomCanvas(
        song: String,
        artist: String,
        album: String? = null,
        animatedUrl: String?,
        videoUrl: String? = null,
        staticUrl: String? = null,
    ): Boolean {
        val entry = CustomCanvasEntry(
            song = song,
            artist = artist,
            album = album?.takeIf { it.isNotBlank() },
            animatedUrl = animatedUrl?.takeIf { it.isNotBlank() },
            videoUrl = videoUrl?.takeIf { it.isNotBlank() },
            staticUrl = staticUrl?.takeIf { it.isNotBlank() },
        )
        val added = synchronized(entries) {
            val existing = entries.indexOfFirst {
                normalize(it.song) == normalize(entry.song) &&
                    normalize(it.artist) == normalize(entry.artist) &&
                    normalize(it.album.orEmpty()) == normalize(entry.album.orEmpty())
            }
            if (existing >= 0) {
                entries[existing] = entry
                false
            } else {
                entries.add(entry)
                true
            }
        }
        schedulePersist()
        return added
    }

    /** Removes the entry registered for the given normalized song/artist/album. */
    fun removeCustomCanvas(song: String, artist: String, album: String? = null): Boolean {
        val removed = synchronized(entries) {
            val iterator = entries.iterator()
            var matched = false
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val albumMatch = album == null ||
                    normalize(entry.album.orEmpty()) == normalize(album)
                if (normalize(entry.song) == normalize(song) &&
                    normalize(entry.artist) == normalize(artist) &&
                    albumMatch
                ) {
                    iterator.remove()
                    matched = true
                }
            }
            matched
        }
        if (removed) schedulePersist()
        return removed
    }

    /** Removes every registered custom canvas. */
    fun clear() {
        synchronized(entries) { entries.clear() }
        schedulePersist()
    }

    /** Current number of registered entries. */
    fun size(): Int = synchronized(entries) { entries.size }

    /**
     * Bulk-imports entries from a JSON document (array of objects with
     * `song`, `artist`, optional `album` and the URL fields). Existing entries
     * for the same song/artist/album are replaced.
     *
     * @return the number of imported entries, or `null` when the document
     *         could not be parsed.
     */
    fun importJson(text: String): Int? {
        val imported = try {
            json.decodeFromString(listSerializer, text)
        } catch (e: Exception) {
            Timber.w(e, "$TAG: failed to import custom canvas json")
            return null
        }
        synchronized(entries) {
            for (entry in imported) {
                val existing = entries.indexOfFirst {
                    normalize(it.song) == normalize(entry.song) &&
                        normalize(it.artist) == normalize(entry.artist) &&
                        normalize(it.album.orEmpty()) == normalize(entry.album.orEmpty())
                }
                if (existing >= 0) entries[existing] = entry else entries.add(entry)
            }
        }
        schedulePersist()
        return imported.size
    }

    /** Serializes the registry to the same JSON schema [importJson] accepts. */
    fun exportJson(): String = synchronized(entries) {
        json.encodeToString(listSerializer, entries.toList())
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (!loaded) {
                loadLocked()
                loaded = true
            }
        }
    }

    /** Must be called while holding [mutex] (or before first use, from init). */
    private suspend fun loadLocked() {
        val file = persistFile ?: run {
            loaded = true
            return
        }
        if (!file.isFile) {
            loaded = true
            return
        }
        try {
            val restored = json.decodeFromString(listSerializer, file.readText())
            synchronized(entries) {
                entries.clear()
                entries.addAll(restored)
            }
            Timber.d("$TAG: restored ${restored.size} custom canvas entries")
        } catch (e: Exception) {
            Timber.w(e, "$TAG: failed to load custom canvas json")
        }
        loaded = true
    }

    private fun schedulePersist() {
        val file = persistFile ?: return
        persistJob?.cancel()
        persistJob = ioScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            try {
                val snapshot = synchronized(entries) { entries.toList() }
                val temp = File(file.parentFile, "${file.name}.tmp")
                temp.writeText(json.encodeToString(listSerializer, snapshot))
                if (file.exists()) file.delete()
                if (!temp.renameTo(file)) {
                    Timber.w("$TAG: could not persist custom canvas json")
                }
            } catch (e: Exception) {
                Timber.w(e, "$TAG: failed to persist custom canvas json")
            }
        }
    }

    private fun entryField(entryValue: String, fallback: String): String =
        entryValue.takeIf { it.isNotBlank() } ?: fallback

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
        .trim()
}
