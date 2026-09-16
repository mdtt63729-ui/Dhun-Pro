/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.dhun.playback.DownloadUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backing view model for [dev.brahmkshatriya.echo.dhun.ui.menu.SongMenu]'s
 * "remove from cache" action when a song is opened from the offline cache.
 */
@HiltViewModel
class CachePlaylistViewModel @Inject constructor(
    private val downloadUtil: DownloadUtil,
) : ViewModel() {

    /**
     * Removes the given song from the offline cache by dropping its
     * media3 download (the same mechanism ExoDownloadService uses).
     */
    fun removeSongFromCache(songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                downloadUtil.downloadManager.removeDownload(songId)
            }
        }
    }
}
