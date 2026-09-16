package dev.brahmkshatriya.echo.dhun.playback.audio

import kotlin.math.PI
import kotlin.math.sin

class PanningProcessor {
    fun process(left: Float, right: Float, phase: Float, depth: Float, out: FloatArray) {
        if (depth <= 0f) {
            out[0] = left
            out[1] = right
            return
        }
        val movement = sin(phase * 2f * PI.toFloat()) * depth
        val lGain = (1f - movement * 0.55f).coerceIn(0.35f, 1.65f)
        val rGain = (1f + movement * 0.55f).coerceIn(0.35f, 1.65f)
        out[0] = left * lGain * 0.985f
        out[1] = right * rGain * 0.985f
    }
}
