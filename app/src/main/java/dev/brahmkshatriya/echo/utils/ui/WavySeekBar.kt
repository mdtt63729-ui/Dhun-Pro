package dev.brahmkshatriya.echo.utils.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import com.google.android.material.slider.Slider
import kotlin.math.PI
import kotlin.math.sin

/**
 * The player seek bar.
 *
 * It extends Material's [Slider] so every existing call — `value`, `valueTo`,
 * `addOnChangeListener`, the tint lists — keeps working, and simply paints a sine wave over the
 * track when a wavy style is selected. That is how the Wavy and Circular styles from the Muso
 * player (its `EchoWavySlider` / `SquigglySlider`) are reproduced here: the base track is made
 * transparent by the style applier and this draws the wave instead.
 *
 * The wave only animates while a song is playing, matching Muso, where the amplitude flattens
 * to zero when playback is paused.
 */
class WavySeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.sliderStyle,
) : Slider(context, attrs, defStyleAttr) {

    /** Draw the wave instead of relying on the (transparent) Material track. */
    var waveEnabled = false

    /** Round the wave's ends — the "Circular" style. */
    var roundedCaps = false

    /** Wave line thickness, in px. */
    var waveStrokePx = 0f

    /** Wave height, in px. Zero draws a straight line. */
    var waveAmplitudePx = 0f

    /** Scroll the wave while playback is running. */
    var waveAnimating = false

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val wavePath = Path()
    private var phase = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!waveEnabled || waveStrokePx <= 0f) return

        val fraction = if (valueTo > valueFrom) {
            ((value - valueFrom) / (valueTo - valueFrom)).coerceIn(0f, 1f)
        } else 0f

        val left = paddingLeft.toFloat()
        val full = (width - paddingLeft - paddingRight).toFloat()
        val right = left + full * fraction
        if (right - left < 1f) return

        wavePaint.strokeWidth = waveStrokePx
        wavePaint.strokeCap = if (roundedCaps) Paint.Cap.ROUND else Paint.Cap.SQUARE
        wavePaint.color = trackActiveTintList?.defaultColor ?: 0xFFFFFFFF.toInt()

        val centerY = height / 2f
        val wavelength = (full / 5f).coerceAtLeast(32f)
        wavePath.reset()
        wavePath.moveTo(left, centerY)
        var x = left
        while (x <= right) {
            val y = centerY + waveAmplitudePx *
                sin((x / wavelength) * 2f * PI.toFloat() + phase)
            wavePath.lineTo(x, y)
            x += 2f
        }
        wavePath.lineTo(right, centerY)
        canvas.drawPath(wavePath, wavePaint)

        if (waveAnimating) {
            phase += 0.09f
            val full = 2f * PI.toFloat()
            if (phase > full) phase -= full
            postInvalidateOnAnimation()
        }
    }
}
