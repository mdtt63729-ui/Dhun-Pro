package dev.brahmkshatriya.echo.dhun.playback.audio

/** Immutable, bounded DSP preset definitions. 8D/16D are effect presets, not audio formats. */
class PresetManager {
    data class Preset(
        val width: Float,
        val panDepth: Float,
        val motionHz: Float,
        val depth: Float,
        val delayMix: Float,
        val reverbMix: Float,
        val wet: Float,
        val outputGain: Float,
    )

    private val normal = Preset(0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f)
    private val spatial3d = Preset(0.12f, 0.06f, 0f, 0.10f, 0.025f, 0.035f, 0.34f, 0.96f)
    private val moving8d = Preset(0.18f, 0.28f, 0.055f, 0.15f, 0.035f, 0.045f, 0.48f, 0.93f)
    private val immersive16d = Preset(0.24f, 0.42f, 0.13f, 0.22f, 0.055f, 0.065f, 0.58f, 0.89f)

    fun preset(mode: AudioMode): Preset = when (mode) {
        AudioMode.NORMAL_2D -> normal
        AudioMode.SPATIAL_3D -> spatial3d
        AudioMode.MOVING_8D -> moving8d
        AudioMode.IMMERSIVE_16D -> immersive16d
    }
}
