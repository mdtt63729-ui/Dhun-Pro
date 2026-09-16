/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.ui.screens.settings

/**
 * Dark-theme preference used across the app shell.
 *
 * Stored through [dev.brahmkshatriya.echo.dhun.constants.DarkModeKey] (string
 * preference holding the enum name) and read via
 * `rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)`.
 */
enum class DarkMode {
    /** Follow the system dark-theme setting. */
    AUTO,

    /** Always render the dark theme. */
    ON,

    /** Always render the light theme. */
    OFF,
}
