package dev.brahmkshatriya.echo.dhun.canvas.models

import kotlinx.serialization.Serializable

@Serializable
data class CanvasArtwork(
    val url: String? = null,
    val videoUrl: String? = null,
    val filePath: String? = null,
    val type: String = "static",
)
