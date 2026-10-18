package dev.brahmkshatriya.echo.utils

/**
 * Preference keys for the settings added from ArchiveTune.
 *
 * Kept together (and deliberately named so they cannot collide with the keys the app already
 * uses) so it is obvious at a glance which settings are new and that none of them duplicate an
 * existing one.
 */
object SettingsKeys {

    // --- Appearance -------------------------------------------------------------------------

    /** A named colour palette; writing it also sets [dev.brahmkshatriya.echo.MainActivity.COLOR_KEY]. */
    const val PALETTE = "palette"
    const val PALETTE_DEFAULT = "default"

    /** Pick a random palette colour each time the app starts. */
    const val RANDOM_THEME = "random_theme"

    /** Skip the backdrop blur entirely (cheap on low-end devices). */
    const val DISABLE_BLUR = "disable_blur"

    /** Backdrop blur radius, 0..100 (%). */
    const val BLUR_AMOUNT = "blur_amount"
    const val BLUR_AMOUNT_DEFAULT = 25

    /** Ask the display for its highest refresh rate. */
    const val FORCE_REFRESH_RATE = "force_refresh_rate"

    /** Hide the artwork on the player, leaving just the controls. */
    const val HIDE_PLAYER_THUMBNAIL = "hide_player_thumbnail"

    /** Corner radius of the player artwork, in dp. */
    const val COVER_RADIUS = "cover_radius"
    const val COVER_RADIUS_DEFAULT = 12

    /** Which bottom-nav tab to open on launch: 0 = Home, 1 = Search, 2 = Library. */
    const val DEFAULT_TAB = "default_tab"
    const val DEFAULT_TAB_HOME = "0"

    /** Haptic feedback on interactive elements. */
    const val HAPTICS = "haptics"

    /** Block screenshots and the recents thumbnail. */
    const val DISABLE_SCREENSHOT = "disable_screenshot"

    // --- Player -----------------------------------------------------------------------------

    /** Hold a partial wake lock while playing. */
    const val KEEP_CPU_AWAKE = "keep_cpu_awake"

    /** Keep shuffle on when a new track starts. */
    const val PERMANENT_SHUFFLE = "permanent_shuffle"

    /** Move on to the next track when the current one fails. */
    const val AUTO_SKIP_ERROR = "auto_skip_error"

    // --- Updates ----------------------------------------------------------------------------

    /** "stable" or "beta". */
    const val UPDATE_CHANNEL = "update_channel"
    const val UPDATE_CHANNEL_STABLE = "stable"

    /** Show a notification when a newer release is found. */
    const val UPDATE_NOTIFICATIONS = "update_notifications"

    // --- Storage ----------------------------------------------------------------------------

    /** Image disk cache ceiling, in MB. */
    const val IMAGE_CACHE_SIZE = "image_cache_size"
    const val IMAGE_CACHE_SIZE_DEFAULT = 250

    // --- Palettes ---------------------------------------------------------------------------

    /**
     * Named palettes added from ArchiveTune. [PALETTE_DEFAULT] means "use the app's own
     * colour"; every other value is a `#AARRGGBB` string written straight into
     * [dev.brahmkshatriya.echo.MainActivity.COLOR_KEY].
     */
    val paletteNames = arrayOf(
        "Default", "Ocean Blue", "Arctic", "Midnight", "Sky Blue", "Cobalt", "Emerald",
        "Teal Wave", "Forest", "Mint", "Olive", "Sage", "Sunset", "Golden", "Amber",
        "Tangerine", "Peach", "Mango", "Royal Purple", "Lavender", "Grape", "Violet",
        "Amethyst", "Sakura", "Rose Quartz", "Magenta", "Hot Pink", "Coral", "Crimson",
        "Wine", "Ruby", "Scarlet", "Charcoal", "Silver", "Slate", "Graphite",
        "Terracotta", "Coffee", "Mocha", "Sand", "Clay", "Cyberpunk", "Synthwave",
        "Aurora", "Twilight"
    )

    val paletteValues = arrayOf(
        PALETTE_DEFAULT,
        "#FF1565C0", "#FFB3E5FC", "#FF1A237E", "#FF03A9F4", "#FF3D5AFE", "#FF00C853",
        "#FF00897B", "#FF2E7D32", "#FF98FF98", "#FF808000", "#FF8A9A5B", "#FFFF7043",
        "#FFFFC107", "#FFFFA000", "#FFFF9800", "#FFFFAB91", "#FFFFB300", "#FF7B1FA2",
        "#FFB39DDB", "#FF6A1B9A", "#FF8E24AA", "#FF9C27B0", "#FFF48FB1", "#FFF8BBD0",
        "#FFE91E63", "#FFFF4081", "#FFFF7043", "#FFD32F2F", "#FF880E4F", "#FFC62828",
        "#FFB71C1C", "#FF37474F", "#FFBDBDBD", "#FF607D8B", "#FF424242", "#FFBF5B3F",
        "#FF6F4E37", "#FF795548", "#FFC2B280", "#FFB66A50", "#FF00E5FF", "#FFFF00FF",
        "#FF00FFA3", "#FF8A2BE2"
    )

    /** A random palette colour (never the "default" entry), as an ARGB int. */
    fun randomPaletteColor(): Int? {
        val hex = paletteValues.drop(1).randomOrNull() ?: return null
        return runCatching { android.graphics.Color.parseColor(hex) }.getOrNull()
    }
}
