package dev.brahmkshatriya.echo.ui.player

import android.content.Context
import android.graphics.Bitmap
import androidx.palette.graphics.Palette
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.MainActivity.Companion.isAmoled
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isNightMode

data class PlayerColors(
    val background: Int,
    val accent: Int,
    val onBackground: Int,
) {
    companion object {
        fun Context.getColorsFrom(bitmap: Bitmap?): PlayerColors? {
            bitmap ?: return null
            val palette = Palette.from(bitmap.paletteSample()).generate()
            return if (!isAmoled()) {
                val lightMode = !isNightMode()
                val lightSwatch = palette.run {
                    lightVibrantSwatch ?: vibrantSwatch ?: lightMutedSwatch
                }
                val darkSwatch = palette.run {
                    darkVibrantSwatch ?: darkMutedSwatch ?: mutedSwatch
                }
                val bgSwatch = if (lightMode) lightSwatch else darkSwatch
                val accentSwatch = if (lightMode) darkSwatch else lightSwatch
                bgSwatch?.run {
                    PlayerColors(rgb, accentSwatch?.rgb ?: titleTextColor, bodyTextColor)
                }
            } else defaultPlayerColors().let { default ->
                val dominantColor = palette.run {
                    vibrantSwatch?.rgb ?: getDominantColor(0).takeIf { it != 0 }
                } ?: return null
                PlayerColors(default.background, dominantColor, default.onBackground)
            }
        }

        fun Context.defaultPlayerColors(): PlayerColors {
            val background = MaterialColors.getColor(
                this, R.attr.navBackground, 0
            )
            val primary = MaterialColors.getColor(
                this, androidx.appcompat.R.attr.colorPrimary, 0
            )
            val onSurface = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnSurface, 0
            )
            return PlayerColors(background, primary, onSurface)
        }

        fun getDominantColor(bitmap: Bitmap): Int {
            return Palette.from(bitmap.paletteSample()).generate().getDominantColor(0)
        }

        /**
         * Palette walks every pixel, so a full-size cover costs hundreds of milliseconds. A
         * small copy picks the same colours and is an order of magnitude cheaper — this is what
         * used to stall the app every time the song (and so the theme colour) changed.
         */
        private const val PALETTE_SAMPLE_SIZE = 128

        private fun Bitmap.paletteSample(): Bitmap =
            if (width > PALETTE_SAMPLE_SIZE || height > PALETTE_SAMPLE_SIZE)
                Bitmap.createScaledBitmap(this, PALETTE_SAMPLE_SIZE, PALETTE_SAMPLE_SIZE, true)
            else this
    }
}