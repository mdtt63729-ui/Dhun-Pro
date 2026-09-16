/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.widget

import android.content.Context
import androidx.media3.common.Player

/**
 * Immutable snapshot of the player state pushed to the home-screen widgets.
 */
data class PlayerWidgetState(
    val mediaId: String?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val thumbnailUrl: String?,
) {
    companion object {
        /**
         * Captures the current state of a media3 [Player].
         *
         * Reads only player-visible state, so it is cheap and safe to call from
         * any dispatcher as long as the caller enforces the player's main-thread
         * contract.
         */
        fun fromPlayer(player: Player, context: Context): PlayerWidgetState {
            val mediaItem = player.currentMediaItem
            val metadata = mediaItem?.mediaMetadata
            return PlayerWidgetState(
                mediaId = mediaItem?.mediaId,
                title = metadata?.title?.toString(),
                artist = metadata?.artist?.toString()
                    ?: metadata?.subtitle?.toString(),
                album = metadata?.albumTitle?.toString(),
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition,
                durationMs = player.duration.coerceAtLeast(0L),
                thumbnailUrl = metadata?.artworkUri?.toString(),
            )
        }
    }
}
