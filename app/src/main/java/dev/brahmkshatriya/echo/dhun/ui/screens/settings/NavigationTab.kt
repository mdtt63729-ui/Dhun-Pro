/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.ui.screens.settings

/**
 * The tabs of the main navigation shell.
 *
 * Stored through [dev.brahmkshatriya.echo.dhun.constants.DefaultOpenTabKey]
 * (string preference holding the enum name) and used by `MainActivity` to pick
 * the `NavHost` start destination and to resolve launcher shortcuts
 * (`ACTION_SEARCH` / `ACTION_LIBRARY`).
 */
enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
}
