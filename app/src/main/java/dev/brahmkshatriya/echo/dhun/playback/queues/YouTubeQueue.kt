/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package dev.brahmkshatriya.echo.dhun.playback.queues

import androidx.media3.common.MediaItem
import dev.brahmkshatriya.echo.dhun.innertube.YouTube
import dev.brahmkshatriya.echo.dhun.innertube.models.WatchEndpoint
import dev.brahmkshatriya.echo.dhun.extensions.toMediaItem
import dev.brahmkshatriya.echo.dhun.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeQueue(
    private var endpoint: WatchEndpoint,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    private var continuation: String? = null

    override suspend fun getInitialStatus(): Queue.Status {
        val nextResult =
            withContext(IO) {
                YouTube.next(endpoint, continuation).getOrThrow()
            }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation
        return Queue.Status(
            title = nextResult.title,
            items = nextResult.items.map { it.toMediaItem() },
            mediaItemIndex = nextResult.currentIndex ?: 0,
        )
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaItem> {
        val nextResult =
            withContext(IO) {
                YouTube.next(endpoint, continuation).getOrThrow()
            }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation
        return nextResult.items.map { it.toMediaItem() }
    }

    companion object {
        fun radio(song: MediaMetadata) = YouTubeQueue(WatchEndpoint(song.id), song)
    }
}
