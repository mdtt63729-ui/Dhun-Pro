/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.ui.screens.settings

/**
 * Horizontal alignment of synced lyrics on the player screen.
 *
 * Stored through [dev.brahmkshatriya.echo.dhun.constants.LyricsTextPositionKey]
 * (string preference holding the enum name) and consumed by
 * `dhun/ui/component/Lyrics.kt`.
 */
enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}
