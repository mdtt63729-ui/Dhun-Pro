package dev.brahmkshatriya.echo.dhun.playback

import dev.brahmkshatriya.echo.dhun.playback.audio.AudioMode
import dev.brahmkshatriya.echo.dhun.playback.audio.AudioModeController
import kotlinx.coroutines.flow.StateFlow

/** Backward-compatible facade for existing playback code. New UI code uses AudioModeController directly. */
object SpatialAudioController {
    typealias Mode = AudioMode
    val mode: StateFlow<AudioMode> = AudioModeController.mode
    fun setMode(mode: AudioMode) = AudioModeController.setMode(mode)
    fun setModeOrdinal(value: Int) = AudioModeController.setModeOrdinal(value)
    fun currentMode(): AudioMode = AudioModeController.currentMode()
}
