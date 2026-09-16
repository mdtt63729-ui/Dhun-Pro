package dev.brahmkshatriya.echo.dhun.playback.audio

class ReverbProcessor {
    private var left = FloatArray(1)
    private var right = FloatArray(1)
    private var index = 0

    fun configure(sampleRate: Int) {
        val size = (sampleRate * 0.011f).toInt().coerceIn(1, 2048)
        left = FloatArray(size)
        right = FloatArray(size)
        index = 0
    }

    fun process(l: Float, r: Float, mix: Float, out: FloatArray) {
        if (mix <= 0f) {
            out[0] = l
            out[1] = r
            return
        }
        val rl = left[index]
        val rr = right[index]
        left[index] = (l + rl * 0.42f).coerceIn(-1f, 1f)
        right[index] = (r + rr * 0.42f).coerceIn(-1f, 1f)
        index++
        if (index >= left.size) index = 0
        out[0] = l * (1f - mix) + rl * mix
        out[1] = r * (1f - mix) + rr * mix
    }

    fun reset() { left.fill(0f); right.fill(0f); index = 0 }
}
