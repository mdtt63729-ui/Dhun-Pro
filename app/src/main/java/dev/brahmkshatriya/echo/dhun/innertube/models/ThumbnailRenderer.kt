/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ThumbnailRenderer(
    @JsonNames("croppedSquareThumbnailRenderer")
    val musicThumbnailRenderer: MusicThumbnailRenderer?,
    val musicAnimatedThumbnailRenderer: MusicAnimatedThumbnailRenderer?,
    val croppedSquareThumbnailRenderer: MusicThumbnailRenderer?,
) {
    @Serializable
    data class MusicThumbnailRenderer(
        val thumbnail: Thumbnails,
        val thumbnailCrop: String?,
        val thumbnailScale: String?,
    ) {
        fun getThumbnailUrl() = thumbnail.thumbnails.maxByOrNull { it.width ?: 0 }?.normalizedUrl
    }

    @Serializable
    data class MusicAnimatedThumbnailRenderer(
        val animatedThumbnail: Thumbnails,
        val backupRenderer: MusicThumbnailRenderer,
    )
}
