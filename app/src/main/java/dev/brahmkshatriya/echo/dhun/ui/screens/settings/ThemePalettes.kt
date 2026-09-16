/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.ui.screens.settings

import androidx.compose.ui.graphics.Color
import dev.brahmkshatriya.echo.dhun.ui.theme.DefaultThemeColor
import kotlin.random.Random

/**
 * Named seed palettes for the theme system.
 *
 * The preference [dev.brahmkshatriya.echo.dhun.constants.CustomThemeColorKey]
 * stores one of three shapes of value:
 *  - a hex color such as `#ED5564` (single seed color),
 *  - a `seedPalette:...` blob produced by
 *    [dev.brahmkshatriya.echo.dhun.ui.theme.ThemeSeedPaletteCodec],
 *  - or the [Palette.id] of one of the palettes below (e.g. `default`, `blue`).
 *
 * `MainActivity` resolves the third shape through [findById] and builds a
 * [dev.brahmkshatriya.echo.dhun.ui.theme.ThemeSeedPalette] from the four
 * seed colors; `App` uses [generateRandomPalette] when the
 * "random theme on startup" preference is enabled.
 */
object ThemePalettes {

    /** A named four-color seed palette. */
    data class Palette(
        val id: String,
        val name: String,
        val primary: Color,
        val secondary: Color,
        val tertiary: Color,
        val neutral: Color,
    )

    private fun palette(
        id: String,
        primary: Long,
        secondary: Long,
        tertiary: Long,
        neutral: Long,
    ) = Palette(
        id = id,
        name = id.replaceFirstChar { it.uppercase() },
        primary = Color(primary),
        secondary = Color(secondary),
        tertiary = Color(tertiary),
        neutral = Color(neutral),
    )

    /** Must match `DefaultThemeColor` so the factory default resolves. */
    val default = Palette(
        id = "default",
        name = "Default",
        primary = DefaultThemeColor,
        secondary = Color(0xFFED7D55),
        tertiary = Color(0xFFEDB355),
        neutral = Color(0xFF8C585C),
    )

    val all: List<Palette> = listOf(
        default,
        palette("red", 0xFFF44336, 0xFFEF9A9A, 0xFFFFCDD2, 0xFF795548),
        palette("pink", 0xFFE91E63, 0xFFF48FB1, 0xFFF8BBD0, 0xFF8D6E63),
        palette("purple", 0xFF9C27B0, 0xFFCE93D8, 0xFFE1BEE7, 0xFF616161),
        palette("deepPurple", 0xFF673AB7, 0xFFB39DDB, 0xFFD1C4E9, 0xFF455A64),
        palette("indigo", 0xFF3F51B5, 0xFF9FA8DA, 0xFFC5CAE9, 0xFF37474F),
        palette("blue", 0xFF2196F3, 0xFF90CAF9, 0xFFBBDEFB, 0xFF546E7A),
        palette("lightBlue", 0xFF03A9F4, 0xFF81D4FA, 0xFFB3E5FC, 0xFF607D8B),
        palette("cyan", 0xFF00BCD4, 0xFF80DEEA, 0xFFB2EBF2, 0xFF546E7A),
        palette("teal", 0xFF009688, 0xFF80CBC4, 0xFFB2DFDB, 0xFF455A64),
        palette("green", 0xFF4CAF50, 0xFFA5D6A7, 0xFFC8E6C9, 0xFF3E2723),
        palette("lightGreen", 0xFF8BC34A, 0xFFC5E1A5, 0xFFDCEDC8, 0xFF4E342E),
        palette("lime", 0xFFCDDC39, 0xFFE6EE9C, 0xFFF0F4C3, 0xFF37474F),
        palette("yellow", 0xFFFFEB3B, 0xFFFFF59D, 0xFFFFF9C4, 0xFF5D4037),
        palette("amber", 0xFFFFC107, 0xFFFFE082, 0xFFFFECB3, 0xFF4E342E),
        palette("orange", 0xFFFF9800, 0xFFFFCC80, 0xFFFFE0B2, 0xFF3E2723),
        palette("deepOrange", 0xFFFF5722, 0xFFFFAB91, 0xFFFFCCBC, 0xFF37474F),
        palette("brown", 0xFF795548, 0xFFBCAAA4, 0xFFD7CCC8, 0xFF3E2723),
        palette("grey", 0xFF9E9E9E, 0xFFBDBDBD, 0xFFE0E0E0, 0xFF424242),
        palette("blueGrey", 0xFF607D8B, 0xFFB0BEC5, 0xFFCFD8DC, 0xFF263238),
    )

    /** Resolves a stored preference value (a palette id) to its [Palette]. */
    fun findById(id: String): Palette? {
        val normalized = id.trim()
        if (normalized.isEmpty()) return null
        return all.firstOrNull { it.id.equals(normalized, ignoreCase = true) }
    }

    /** Finds a palette by id, falling back to [default]. */
    fun getByIdOrDefault(id: String): Palette = findById(id) ?: default

    /**
     * Generates a fresh random palette for the "random theme on startup"
     * preference. Derived from a random hue so each startup feels different
     * while staying visually coherent.
     */
    fun generateRandomPalette(): Palette {
        val hue = Random.nextFloat() * 360f
        val primary = Color.hsv(hue, 0.62f, 0.86f)
        val secondary = Color.hsv((hue + 40f) % 360f, 0.45f, 0.80f)
        val tertiary = Color.hsv((hue + 80f) % 360f, 0.55f, 0.84f)
        val neutral = Color.hsv(hue, 0.08f, 0.42f)
        return Palette(
            id = "random",
            name = "Random",
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            neutral = neutral,
        )
    }
}
