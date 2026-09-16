package dev.brahmkshatriya.echo.dhun.playback.audio

import kotlin.math.abs
import kotlin.math.max

class Limiter(private val safePeak: Float = 0.96f) {
    fun process(left: Float, right: Float, gain: Float): FloatArray {
        var l = left * gain
        var r = right * gain
        val peak = max(abs(l), abs(r))
        if (peak > safePeak) {
            val scale = safePeak / peak
            l *= scale
            r *= scale
        }
        return floatArrayOf(l.coerceIn(-safePeak, safePeak), r.coerceIn(-safePeak, safePeak))
    }
}
