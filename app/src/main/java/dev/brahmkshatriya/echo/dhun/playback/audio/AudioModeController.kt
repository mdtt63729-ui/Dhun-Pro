package dev.brahmkshatriya.echo.dhun.playback.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Single source of truth between the player UI and the reusable real-time audio engine. */
object AudioModeController {
    private val atomicMode = AtomicInteger(AudioMode.NORMAL_2D.ordinal)
    private val _mode = MutableStateFlow(AudioMode.NORMAL_2D)
    private val movementSpeed = AtomicReference(1f)
    val mode: StateFlow<AudioMode> = _mode

    fun setMode(mode: AudioMode) {
        atomicMode.set(mode.ordinal)
        _mode.value = mode
    }

    fun setModeOrdinal(value: Int) = setMode(AudioMode.fromOrdinal(value))
    fun currentMode(): AudioMode = AudioMode.fromOrdinal(atomicMode.get())
    fun next(): AudioMode = AudioMode.fromOrdinal((atomicMode.get() + 1).coerceAtMost(AudioMode.entries.lastIndex))
    fun previous(): AudioMode = AudioMode.fromOrdinal((atomicMode.get() - 1).coerceAtLeast(0))
    fun setMovementSpeed(multiplier: Float) = movementSpeed.set(multiplier.coerceIn(0.25f, 2f))
    fun movementSpeed(): Float = movementSpeed.get()
}
