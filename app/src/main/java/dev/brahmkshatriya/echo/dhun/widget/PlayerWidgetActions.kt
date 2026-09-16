/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.widget

/**
 * Intent actions handled by the player widget layer.
 *
 * [dev.brahmkshatriya.echo.dhun.playback.MusicService] receives these actions
 * through `onStartCommand` (the widgets attach `PendingIntent.getService`
 * callbacks) and applies them to the player.
 */
object PlayerWidgetActions {
    const val ACTION_PLAY_PAUSE = "dev.brahmkshatriya.echo.dhun.widget.action.PLAY_PAUSE"
    const val ACTION_NEXT = "dev.brahmkshatriya.echo.dhun.widget.action.NEXT"
    const val ACTION_PREVIOUS = "dev.brahmkshatriya.echo.dhun.widget.action.PREVIOUS"
    const val ACTION_REFRESH = "dev.brahmkshatriya.echo.dhun.widget.action.REFRESH"
}
