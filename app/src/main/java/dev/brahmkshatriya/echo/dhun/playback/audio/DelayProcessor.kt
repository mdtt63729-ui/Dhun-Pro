package dev.brahmkshatriya.echo.dhun.playback.audio

class DelayProcessor {
    private var left = FloatArray(1)
    private var right = FloatArray(1)
    private var index = 0

    fun configure(sampleRate: Int) {
        val size = (sampleRate * 0.045f).toInt().coerceIn(1, 4096)
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
        val dl = left[index]
        val dr = right[index]
        left[index] = (l + dl * 0.16f).coerceIn(-1f, 1f)
        right[index] = (r + dr * 0.16f).coerceIn(-1f, 1f)
        index++
        if (index >= left.size) index = 0
        out[0] = l * (1f - mix) + dr * mix
        out[1] = r * (1f - mix) + dl * mix
    }

    fun reset() { left.fill(0f); right.fill(0f); index = 0 }
}
