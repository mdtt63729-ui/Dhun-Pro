/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.dhun.db.MusicDatabase
import dev.brahmkshatriya.echo.dhun.db.entities.LyricsEntity
import dev.brahmkshatriya.echo.dhun.lyrics.LyricsHelper
import dev.brahmkshatriya.echo.dhun.lyrics.LyricsResult
import dev.brahmkshatriya.echo.dhun.models.MediaMetadata
import dev.brahmkshatriya.echo.dhun.utils.NetworkConnectivityObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View model backing the LyricsMenu bottom sheet: lyrics search across
 * providers, editing, translating and refetching lyrics.
 */
@HiltViewModel
class LyricsMenuViewModel @Inject constructor(
    private val lyricsHelper: LyricsHelper,
    private val database: MusicDatabase,
    networkConnectivity: NetworkConnectivityObserver,
) : ViewModel() {

    val isNetworkAvailable: StateFlow<Boolean> = flow {
        emit(networkConnectivity.isCurrentlyConnected())
        networkConnectivity.networkStatus.collect { emit(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _results = MutableStateFlow<List<LyricsResult>>(emptyList())
    val results: StateFlow<List<LyricsResult>> = _results

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var searchJob: Job? = null

    /** Searches all enabled lyrics providers for the given song. */
    fun search(mediaId: String, title: String, artist: String, duration: Int) {
        searchJob?.cancel()
        _results.value = emptyList()
        _isLoading.value = true
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                runCatching {
                    lyricsHelper.getAllLyrics(mediaId, title, artist, null, duration) { result ->
                        _results.value =
                            _results.value.filterNot { it == result } + result
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Cancels an in-flight lyrics search. */
    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
        lyricsHelper.cancelCurrentLyricsJob()
        _isLoading.value = false
    }

    /** Persists the (edited / translated / picked) lyrics for the song. */
    fun updateLyrics(mediaMetadata: MediaMetadata, lyrics: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = lyrics.trim()
            database.upsert(LyricsEntity(mediaMetadata.id, trimmed))
        }
    }

    /**
     * Drops the stored lyrics for the song and re-fetches them from the
     * lyrics providers.
     */
    fun refetchLyrics(mediaMetadata: MediaMetadata, lyricsEntity: LyricsEntity?) {
        viewModelScope.launch(Dispatchers.IO) {
            lyricsEntity?.let { database.delete(it) }
            val fresh = runCatching { lyricsHelper.getLyrics(mediaMetadata) }.getOrNull()
            if (!fresh.isNullOrBlank() && fresh != LyricsEntity.LYRICS_NOT_FOUND) {
                database.upsert(LyricsEntity(mediaMetadata.id, fresh))
            }
        }
    }
}
