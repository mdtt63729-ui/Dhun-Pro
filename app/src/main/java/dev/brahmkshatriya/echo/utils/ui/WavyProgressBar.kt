package dev.brahmkshatriya.echo.utils.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlin.math.PI
import kotlin.math.sin

/**
 * The mini player's progress line.
 *
 * Extends Material's [LinearProgressIndicator] — so `progress`, `max`, `trackThickness` and the
 * tint all keep working — and paints a sine wave over the track when a wavy slider style is
 * selected. This is the mini-player counterpart of [WavySeekBar], matching the Wavy and
 * Circular styles from the Muso player.
 */
class WavyProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int =
        com.google.android.material.R.attr.linearProgressIndicatorStyle,
) : LinearProgressIndicator(context, attrs, defStyleAttr) {

    var waveEnabled = false
    var roundedCaps = false
    var waveStrokePx = 0f
    var waveAmplitudePx = 0f
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

        val fraction = if (max > 0) (progress.toFloat() / max).coerceIn(0f, 1f) else 0f
        val full = width.toFloat()
        val right = full * fraction
        if (right < 1f) return

        wavePaint.strokeWidth = waveStrokePx
        wavePaint.strokeCap = if (roundedCaps) Paint.Cap.ROUND else Paint.Cap.SQUARE
        wavePaint.color = indicatorColor.firstOrNull() ?: 0xFFFFFFFF.toInt()

        val centerY = height / 2f
        val wavelength = (full / 5f).coerceAtLeast(32f)
        wavePath.reset()
        wavePath.moveTo(0f, centerY)
        var x = 0f
        while (x <= right) {
            val y = centerY + waveAmplitudePx * sin((x / wavelength) * 2f * PI.toFloat() + phase)
            wavePath.lineTo(x, y)
            x += 2f
        }
        wavePath.lineTo(right, centerY)
        canvas.drawPath(wavePath, wavePaint)

        if (waveAnimating) {
            phase += 0.09f
            val fullTurn = 2f * PI.toFloat()
            if (phase > fullTurn) phase -= fullTurn
            postInvalidateOnAnimation()
        }
    }
}
