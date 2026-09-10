/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package dev.brahmkshatriya.echo.dhun.lyrics

import android.content.Context
import android.util.Log
import android.util.LruCache
import dev.brahmkshatriya.echo.dhun.canvas.providers.GlobalLog
import dev.brahmkshatriya.echo.dhun.constants.PreferredLyricsProvider
import dev.brahmkshatriya.echo.dhun.constants.PreferredLyricsProviderKey
import dev.brahmkshatriya.echo.dhun.constants.ProviderOrderKey
import dev.brahmkshatriya.echo.dhun.constants.UseAITranslationKey
import dev.brahmkshatriya.echo.dhun.constants.TranslateLyricsKey
import dev.brahmkshatriya.echo.dhun.db.DatabaseDao
import dev.brahmkshatriya.echo.dhun.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import dev.brahmkshatriya.echo.dhun.extensions.toEnum
import dev.brahmkshatriya.echo.dhun.models.MediaMetadata
import dev.brahmkshatriya.echo.dhun.utils.dataStore
import dev.brahmkshatriya.echo.dhun.utils.reportException
import dev.brahmkshatriya.echo.dhun.utils.NetworkConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
    private val databaseDao: DatabaseDao,
) {
    private val baseProviders =
        listOf(
            SimpMusicLyricsProvider,
            BetterLyricsProvider,
            LrcLibLyricsProvider,
            KuGouLyricsProvider,
            YouTubeSubtitleLyricsProvider,
            YouTubeLyricsProvider,
        )

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(mediaMetadata: MediaMetadata, preferredProviderOnly: Boolean = false): String {
        currentLyricsJob?.cancel()

        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            GlobalLog.append(Log.DEBUG, "LyricsHelper", "Found lyrics in cache for ${mediaMetadata.title}")
            return cached.lyrics
        }
        
        GlobalLog.append(Log.DEBUG, "LyricsHelper", "Fetching lyrics for ${mediaMetadata.title} (Artist: ${mediaMetadata.artists.joinToString { it.name }}, Album: ${mediaMetadata.album?.title})")

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        
        if (!isNetworkAvailable) {
            GlobalLog.append(Log.WARN, "LyricsHelper", "Network unavailable, aborting lyrics fetch")
            return LYRICS_NOT_FOUND
        }

        val ordered = orderedProviders()
        val providers = if (preferredProviderOnly) listOf(ordered.first()) else ordered
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val deferred = scope.async {
            for (provider in providers) {
                val enabled = provider.isEnabled(context)
                
                if (enabled) {
                    try {
                        val result = provider.getLyrics(
                            mediaMetadata.id,
                            mediaMetadata.title,
                            mediaMetadata.artists.joinToString { it.name },
                            mediaMetadata.album?.title,
                            mediaMetadata.duration,
                        )
                        result.onSuccess { lyrics ->
                            if (isMeaningfulLyrics(lyrics)) {
                                return@async lyrics
                            }
                        }.onFailure {
                            reportException(it)
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            return@async LYRICS_NOT_FOUND
        }

        val lyrics = deferred.await()
        scope.cancel()

        // AI translation fallback: if AI translation is enabled and we got
        // meaningful lyrics, try to get translated lyrics from AI
        if (lyrics != LYRICS_NOT_FOUND) {
            tryAITranslation(mediaMetadata.id, lyrics)
        }

        return lyrics
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        songAlbum: String?,
        duration: Int,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        
        if (!isNetworkAvailable) {
            return
        }

        val allResult = mutableListOf<LyricsResult>()
        val providers = orderedProviders()
        currentLyricsJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
            providers.forEach { provider ->
                if (provider.isEnabled(context)) {
                    try {
                        provider.getAllLyrics(mediaId, songTitle, songArtists, songAlbum, duration) lyricsCallback@{ lyrics ->
                            if (!isMeaningfulLyrics(lyrics)) return@lyricsCallback
                            val result = LyricsResult(provider.name, lyrics)
                            allResult += result
                            callback(result)
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            cache.put(cacheKey, allResult)
        }

        currentLyricsJob?.join()
    }

    private fun PreferredLyricsProvider.toLyricsProvider(): LyricsProvider = when (this) {
        PreferredLyricsProvider.LRCLIB -> LrcLibLyricsProvider
        PreferredLyricsProvider.KUGOU -> KuGouLyricsProvider
        PreferredLyricsProvider.BETTER_LYRICS -> BetterLyricsProvider
        PreferredLyricsProvider.SIMPMUSIC -> SimpMusicLyricsProvider
    }

    private suspend fun orderedProviders(): List<LyricsProvider> {
        val savedOrder = context.dataStore.data
            .first()[ProviderOrderKey]
            ?.split(",")
            ?.mapNotNull { name -> runCatching { PreferredLyricsProvider.valueOf(name) }.getOrNull() }
            ?.map { it.toLyricsProvider() }

        if (!savedOrder.isNullOrEmpty()) {
            val allProviders = savedOrder.toMutableList()
            PreferredLyricsProvider.entries.forEach { enumProvider ->
                val provider = enumProvider.toLyricsProvider()
                if (provider !in allProviders) allProviders.add(provider)
            }
            return allProviders
        }

        val preferred = context.dataStore.data
            .first()[PreferredLyricsProviderKey]
            .toEnum(PreferredLyricsProvider.LRCLIB)

        val first = preferred.toLyricsProvider()
        return listOf(first) + baseProviders.filterNot { it == first }
    }

    private fun isMeaningfulLyrics(lyrics: String): Boolean {
        val normalized =
            lyrics
                .replace("\uFEFF", "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        if (normalized.isEmpty()) return false
        if (normalized == LYRICS_NOT_FOUND) return false

        val remaining =
            TIMESTAMP_REGEX
                .replace(normalized, "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        return remaining.any { !it.isWhitespace() && it != '\u00A0' }
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    // ── AI Translation ─────────────────────────────────────────────────────

    /**
     * Flow that emits the current AI-translated lyrics for the playing song,
     * or null if AI translation is disabled/not yet fetched.
     *
     * UI components collect this to show the "AI Translated" badge and
     * display translated lyrics alongside the original.
     */
    private val _aiTranslatedLyrics = MutableStateFlow<String?>(null)
    val aiTranslatedLyrics: StateFlow<String?> = _aiTranslatedLyrics

    /**
     * Whether the currently displayed lyrics were translated by AI.
     * Used by UI to show the "AI Translated" badge.
     */
    private val _isUsingAITranslation = MutableStateFlow(false)
    val isUsingAITranslation: StateFlow<Boolean> = _isUsingAITranslation

    /**
     * Attempts to get AI-translated lyrics for the current song.
     *
     * This is called as a fallback after any lyrics source provides lyrics.
     * It checks:
     * 1. AI translation is enabled (useAITranslation == true)
     * 2. API key is set
     * 3. Non-AI translation is not already enabled
     *
     * If conditions are met, it checks the local cache first, then calls
     * the AI API. The result is exposed via [aiTranslatedLyrics] flow.
     */
    private suspend fun tryAITranslation(videoId: String, lyrics: String) {
        val data = context.dataStore.data.first()
        val useAI = data[UseAITranslationKey] ?: false
        val apiKey = data[dev.brahmkshatriya.echo.dhun.constants.AIApiKeyKey] ?: ""
        val enableTranslateLyric = data[TranslateLyricsKey] ?: false

        if (!useAI || apiKey.isEmpty() || enableTranslateLyric) {
            _aiTranslatedLyrics.value = null
            _isUsingAITranslation.value = false
            return
        }

        val translationLanguage =
            data[dev.brahmkshatriya.echo.dhun.constants.AITranslationLanguageKey] ?: "en"

        val repository = AITranslationRepository(context, databaseDao)
        val translated = repository.getTranslatedLyrics(videoId, lyrics)

        if (translated != null) {
            _aiTranslatedLyrics.value = translated
            _isUsingAITranslation.value = true
            GlobalLog.append(
                Log.DEBUG,
                "LyricsHelper",
                "AI translation applied for $videoId:$translationLanguage",
            )
        } else {
            _aiTranslatedLyrics.value = null
            _isUsingAITranslation.value = false
        }
    }

    /**
     * Clears AI translation state when switching songs.
     */
    fun clearAITranslation() {
        _aiTranslatedLyrics.value = null
        _isUsingAITranslation.value = false
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3
        private val TIMESTAMP_REGEX = Regex("""\[[0-9]{1,2}:[0-9]{2}(?:\.[0-9]{1,3})?]""")
        private val INVISIBLE_CHARS_REGEX = Regex("""[\u200B\u200C\u200D\u2060\u00AD]""")
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
