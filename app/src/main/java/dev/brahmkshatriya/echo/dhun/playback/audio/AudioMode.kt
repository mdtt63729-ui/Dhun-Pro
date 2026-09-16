package dev.brahmkshatriya.echo.dhun.playback.audio

enum class AudioMode(val label: String) {
    NORMAL_2D("2D"),
    SPATIAL_3D("3D"),
    MOVING_8D("8D"),
    IMMERSIVE_16D("16D");

    companion object {
        fun fromOrdinal(value: Int): AudioMode = entries.getOrElse(value) { NORMAL_2D }
    }
}
