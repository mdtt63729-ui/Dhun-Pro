package dev.brahmkshatriya.echo.utils.ui

import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import dev.brahmkshatriya.echo.databinding.ItemPlayerControlsBinding
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import dev.brahmkshatriya.echo.utils.SettingsKeys
import dev.brahmkshatriya.echo.databinding.ItemPlayerCollapsedBinding
import dev.brahmkshatriya.echo.databinding.ItemPlayerCollapsedControlsBinding
import android.content.res.ColorStateList
import androidx.core.graphics.ColorUtils

/**
 * Applies a now-playing style to the player controls.
 *
 * The app's player is one XML layout, so each style is a preset of real, visible differences —
 * artwork corner radius, how heavy the title/artist block is and how it is aligned, the size of
 * the like button, the spacing around them and how thick the seek bar is. The three alternates
 * are modelled on the now-playing styles in the Muso player (Spotify, Apple Music, Material 3
 * Expressive); [SettingsKeys.PLAYER_STYLE_MATERIAL3] is the app's own look and resets
 * everything back to the layout's defaults.
 *
 * Nothing here recreates a view, so a style can be switched while a song is playing.
 */
object PlayerStyle {

    /**
     * Corner radius of the player artwork, in dp, or -1 to fall back to the user's
     * "Thumbnail corner radius" setting.
     */
    fun coverRadiusDp(style: String): Int = when (style) {
        SettingsKeys.PLAYER_STYLE_SPOTIFY -> 6
        SettingsKeys.PLAYER_STYLE_APPLE -> 16
        SettingsKeys.PLAYER_STYLE_M3E -> 28
        else -> -1
    }

    fun apply(binding: ItemPlayerControlsBinding, style: String) {
        val context = binding.root.context
        val density = context.resources.displayMetrics.density
        val scaled = context.resources.displayMetrics.scaledDensity

        fun sp(value: Float) = value * scaled
        fun dp(value: Float) = value * density

        val title = binding.trackTitle
        val artist = binding.trackArtist
        val heart = binding.trackHeart
        val seek = binding.seekBar
        val buffer = binding.bufferBar

        // The info row is the controls layout's first child.
        val row = binding.root.getChildAt(0) as? LinearLayout

        val titleSp: Float
        val artistSp: Float
        val titleGravity: Int
        val heartDp: Float
        val rowPaddingDp: Float
        val trackDp: Float
        val thumbDp: Float

        when (style) {
            SettingsKeys.PLAYER_STYLE_SPOTIFY -> {
                // Tight and left-aligned, a hairline seek bar, a smaller like button.
                titleSp = 17f; artistSp = 13f; titleGravity = Gravity.START
                heartDp = 56f; rowPaddingDp = 18f; trackDp = 1.5f; thumbDp = 14f
            }

            SettingsKeys.PLAYER_STYLE_APPLE -> {
                // Bigger type, centred, generous spacing, a heavier slider.
                titleSp = 24f; artistSp = 17f; titleGravity = Gravity.CENTER_HORIZONTAL
                heartDp = 72f; rowPaddingDp = 30f; trackDp = 3f; thumbDp = 22f
            }

            SettingsKeys.PLAYER_STYLE_M3E -> {
                // Expressive: a rounded card look, chunky tonal slider, codec capsule below.
                titleSp = 22f; artistSp = 15f; titleGravity = Gravity.START
                heartDp = 64f; rowPaddingDp = 24f; trackDp = 5f; thumbDp = 26f
            }

            else -> {
                // Material 3 — the layout's own values.
                titleSp = 20f; artistSp = 16f; titleGravity = Gravity.BOTTOM or Gravity.START
                heartDp = 64f; rowPaddingDp = 24f; trackDp = 2f; thumbDp = 20f
            }
        }

        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSp)
        title.gravity = titleGravity
        artist.setTextSize(TypedValue.COMPLEX_UNIT_SP, artistSp)
        artist.gravity = if (titleGravity == Gravity.CENTER_HORIZONTAL) {
            Gravity.CENTER_HORIZONTAL
        } else {
            Gravity.START
        }

        heart.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            width = dp(heartDp).toInt()
            height = dp(heartDp).toInt()
        }
        row?.setPaddingRelative(
            dp(rowPaddingDp).toInt(), row?.paddingTop ?: 0,
            dp(rowPaddingDp).toInt(), row?.paddingBottom ?: 0
        )

        runCatching {
            seek.trackHeight = dp(trackDp).toInt()
            seek.thumbWidth = dp(thumbDp).toInt()
            seek.thumbHeight = dp(thumbDp).toInt()
            buffer.trackThickness = dp(trackDp).toInt()
        }

        // Material 3 Expressive puts a codec capsule under the seek bar, exactly like Muso's.
        binding.statsText.isVisible = style == SettingsKeys.PLAYER_STYLE_M3E ||
            context.getSettings().getBoolean(SettingsKeys.NERD_STATS, false)
    }

    /**
     * Added from Muso: the seek bar style (its `SliderStyle` enum). Applied after [apply] so it
     * wins over the now-playing style's own track/thumb sizing.
     *
     * Standard is Material's own slider; Thick and Simple are just a fatter/thinner track with
     * the thumb hidden; Wavy and Circular paint a sine wave instead of a track.
     */
    fun applySlider(binding: ItemPlayerControlsBinding, style: String) {
        val context = binding.root.context
        val density = context.resources.displayMetrics.density
        fun dp(value: Float) = (value * density).toInt()

        val seek = binding.seekBar
        val buffer = binding.bufferBar

        when (style) {
            SettingsKeys.SLIDER_STYLE_WAVY, SettingsKeys.SLIDER_STYLE_CIRCULAR -> {
                seek.waveEnabled = true
                seek.roundedCaps = style == SettingsKeys.SLIDER_STYLE_CIRCULAR
                seek.waveStrokePx = dp(6f).toFloat()
                seek.waveAmplitudePx = dp(2f).toFloat()
                // 1px rather than 0: Material divides by the thumb size while drawing.
                seek.thumbWidth = 1
                seek.thumbHeight = 1
                seek.trackHeight = 1
                buffer.trackThickness = 1
            }

            SettingsKeys.SLIDER_STYLE_THICK -> {
                seek.waveEnabled = false
                seek.thumbWidth = 1
                seek.thumbHeight = 1
                seek.trackHeight = dp(12f)
                buffer.trackThickness = dp(12f)
            }

            SettingsKeys.SLIDER_STYLE_SIMPLE -> {
                seek.waveEnabled = false
                seek.thumbWidth = 1
                seek.thumbHeight = 1
                seek.trackHeight = dp(3f)
                buffer.trackThickness = dp(3f)
            }

            else -> {
                seek.waveEnabled = false
                seek.thumbWidth = dp(20f)
                seek.thumbHeight = dp(20f)
                seek.trackHeight = dp(2f)
                buffer.trackThickness = dp(2f)
            }
        }
        seek.requestLayout()
        seek.invalidate()
    }

    /** The wavy styles only scroll while a song is actually playing, like Muso. */
    fun setSliderAnimating(binding: ItemPlayerControlsBinding, playing: Boolean) {
        val seek = binding.seekBar
        if (seek.waveAnimating == playing) return
        seek.waveAnimating = playing
        if (playing) seek.invalidate()
    }

    /**
     * Added from Muso: the mini player's progress line follows the slider style too, so the
     * collapsed bar matches whatever the full player is using.
     */
    fun applyCollapsedControls(
        binding: ItemPlayerCollapsedControlsBinding,
        sliderStyle: String,
    ) {
        val context = binding.root.context
        val density = context.resources.displayMetrics.density
        fun dp(value: Float) = (value * density).toInt()

        val seek = binding.collapsedSeekbar
        val buffer = binding.collapsedBuffer

        when (sliderStyle) {
            SettingsKeys.SLIDER_STYLE_WAVY, SettingsKeys.SLIDER_STYLE_CIRCULAR -> {
                seek.waveEnabled = true
                seek.roundedCaps = sliderStyle == SettingsKeys.SLIDER_STYLE_CIRCULAR
                seek.waveStrokePx = dp(3f).toFloat()
                seek.waveAmplitudePx = dp(1f).toFloat()
                // 1px rather than 0: Material divides by the thickness while drawing.
                seek.trackThickness = 1
                buffer.trackThickness = 1
            }

            SettingsKeys.SLIDER_STYLE_THICK -> {
                seek.waveEnabled = false
                seek.trackThickness = dp(6f)
                buffer.trackThickness = dp(6f)
            }

            SettingsKeys.SLIDER_STYLE_SIMPLE -> {
                seek.waveEnabled = false
                seek.trackThickness = dp(1.5f)
                buffer.trackThickness = dp(1.5f)
            }

            else -> {
                seek.waveEnabled = false
                seek.trackThickness = dp(2f)
                buffer.trackThickness = dp(2f)
            }
        }
        seek.requestLayout()
        seek.invalidate()
    }

    /** The mini player's wavy line only scrolls while a song is playing. */
    fun setCollapsedAnimating(binding: ItemPlayerCollapsedControlsBinding, playing: Boolean) {
        val seek = binding.collapsedSeekbar
        if (seek.waveAnimating == playing) return
        seek.waveAnimating = playing
        if (playing) seek.invalidate()
    }

    /**
     * Added from Muso: the mini player's own styles (its `MiniPlayerStyle` enum), plus the
     * app's default. "minify" is the compact bar; "classic" is the taller Material 3 one.
     *
     * This is the title block half; [applyMiniPlayerControls] does the bar itself.
     */
    fun applyMiniPlayerInfo(binding: ItemPlayerCollapsedBinding, style: String) {
        val context = binding.root.context
        val density = context.resources.displayMetrics.density
        fun dp(value: Float) = (value * density).toInt()

        val titleSp: Float
        val artistSp: Float
        val horizontalDp: Float

        when (style) {
            SettingsKeys.MINI_PLAYER_STYLE_MINIFY -> {
                titleSp = 14f; artistSp = 11f; horizontalDp = 12f
            }

            SettingsKeys.MINI_PLAYER_STYLE_CLASSIC -> {
                titleSp = 17f; artistSp = 13f; horizontalDp = 20f
            }

            else -> {
                titleSp = 16f; artistSp = 14f; horizontalDp = 16f
            }
        }

        binding.collapsedTrackTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSp)
        binding.collapsedTrackArtist.setTextSize(TypedValue.COMPLEX_UNIT_SP, artistSp)

        val row = binding.collapsedPlayerInfo
        row.setPaddingRelative(
            dp(horizontalDp).toInt(), row.paddingTop,
            dp(horizontalDp).toInt(), row.paddingBottom
        )
    }

    /**
     * The bar half of the mini player style. "Classic" also washes the bar with the artwork
     * colour, which is what Muso's Classic mini player does.
     */
    fun applyMiniPlayerControls(
        binding: ItemPlayerCollapsedControlsBinding,
        style: String,
        accent: Int?,
    ) {
        binding.root.backgroundTintList = if (
            style == SettingsKeys.MINI_PLAYER_STYLE_CLASSIC && accent != null
        ) {
            ColorStateList.valueOf(ColorUtils.setAlphaComponent(accent, 64))
        } else null
        binding.root.invalidate()
    }

    /** The mini player style currently selected in settings. */
    fun currentMini(context: android.content.Context): String =
        context.getSettings().getString(
            SettingsKeys.MINI_PLAYER_STYLE, SettingsKeys.MINI_PLAYER_STYLE_DEFAULT
        ) ?: SettingsKeys.MINI_PLAYER_STYLE_DEFAULT

    /** The slider style currently selected in settings. */
    fun currentSlider(context: android.content.Context): String =
        context.getSettings().getString(
            SettingsKeys.SLIDER_STYLE, SettingsKeys.SLIDER_STYLE_STANDARD
        ) ?: SettingsKeys.SLIDER_STYLE_STANDARD

    /** The style currently selected in settings. */
    fun current(context: android.content.Context): String =
        context.getSettings().getString(
            SettingsKeys.PLAYER_STYLE, SettingsKeys.PLAYER_STYLE_MATERIAL3
        ) ?: SettingsKeys.PLAYER_STYLE_MATERIAL3
}
