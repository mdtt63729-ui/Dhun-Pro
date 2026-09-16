package dev.brahmkshatriya.echo.dhun.playback.audio

class StereoProcessor {
    fun process(left: Float, right: Float, width: Float, out: FloatArray) {
        if (width <= 0f) {
            out[0] = left
            out[1] = right
            return
        }
        val mid = (left + right) * 0.5f
        val side = (left - right) * 0.5f
        val bounded = (side * (1f + width)).coerceIn(-0.92f, 0.92f)
        out[0] = (mid + bounded).coerceIn(-1.25f, 1.25f)
        out[1] = (mid - bounded).coerceIn(-1.25f, 1.25f)
    }
}
