package dev.brahmkshatriya.echo.dhun.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicInteger

/** Lightweight, process-wide state shared by the player UI and the audio render pipeline. */
object SpatialAudioController {
    enum class Mode(val label: String, val description: String) {
        TWO_D("2D", "Standard stereo · no spatial processing"),
        THREE_D("3D", "Spatial depth · subtle room ambience"),
        EIGHT_D("8D", "Slow binaural movement · medium ambience"),
        SIXTEEN_D("16D", "Fast binaural movement · wide ambience");

        companion object {
            fun fromOrdinal(value: Int) = entries.getOrElse(value) { TWO_D }
        }
    }

    private val atomicMode = AtomicInteger(Mode.TWO_D.ordinal)
    private val _mode = MutableStateFlow(Mode.TWO_D)
    val mode: StateFlow<Mode> = _mode

    fun setMode(mode: Mode) {
        atomicMode.set(mode.ordinal)
        _mode.value = mode
    }

    fun setModeOrdinal(value: Int) = setMode(Mode.fromOrdinal(value))

    fun currentMode(): Mode = Mode.fromOrdinal(atomicMode.get())
}
