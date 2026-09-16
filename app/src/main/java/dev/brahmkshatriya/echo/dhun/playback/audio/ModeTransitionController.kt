package dev.brahmkshatriya.echo.dhun.playback.audio

import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/** Audio-thread friendly transition state; no per-buffer object allocation. */
class ModeTransitionController(private val transitionFrames: Int = 256) {
    private val targetOrdinal = AtomicInteger(AudioMode.NORMAL_2D.ordinal)
    @Volatile private var currentOrdinal = AudioMode.NORMAL_2D.ordinal
    @Volatile private var fromOrdinal = AudioMode.NORMAL_2D.ordinal
    @Volatile var amount: Float = 1f
        private set
    private val step = 1f / max(1, transitionFrames).toFloat()

    fun setTarget(mode: AudioMode) { targetOrdinal.set(mode.ordinal) }
    fun target(): AudioMode = AudioMode.fromOrdinal(targetOrdinal.get())
    fun current(): AudioMode = AudioMode.fromOrdinal(currentOrdinal)
    fun from(): AudioMode = AudioMode.fromOrdinal(fromOrdinal)

    fun step() {
        val requested = targetOrdinal.get()
        if (requested != currentOrdinal && amount >= 1f) {
            fromOrdinal = currentOrdinal
            currentOrdinal = requested
            amount = 0f
        }
        amount = (amount + step).coerceAtMost(1f)
    }

    fun reset(mode: AudioMode = AudioMode.NORMAL_2D) {
        targetOrdinal.set(mode.ordinal)
        currentOrdinal = mode.ordinal
        fromOrdinal = mode.ordinal
        amount = 1f
    }
}
