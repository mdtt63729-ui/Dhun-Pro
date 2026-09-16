/*
 * Dhun Project (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.canvas.models

import kotlinx.serialization.Serializable

/**
 * Describes an animated ("canvas") artwork for a song.
 *
 * A canvas artwork is a short, loopable video that replaces the static cover
 * in the player UI. The model is provider-agnostic: it only stores the resolved
 * URLs plus some metadata about where it came from.
 *
 * This class is [Serializable] (kotlinx.serialization) because
 * [dev.brahmkshatriya.echo.dhun.ui.player.CanvasArtworkPlaybackCache] persists
 * a `Map<String, CanvasArtwork>` through `MapSerializer(String.serializer(),
 * CanvasArtwork.serializer())`, which requires the plugin-generated serializer.
 *
 * @property id          Provider-specific id of the resolved artwork (song/album/video id), if any.
 * @property songTitle   Title of the song the artwork was resolved for (raw, as passed to the provider).
 * @property artist      Primary artist name the artwork was resolved for.
 * @property album       Album name, when known.
 * @property animated    Primary animation URL. Usually an HLS (`.m3u8`) stream, but may be a
 *                       progressive `.mp4` for some providers. This is what the player prefers.
 * @property videoUrl    Fallback progressive (`.mp4`/`.webm`) URL for the same animation, when available.
 * @property staticUrl   Static (image) artwork URL matching the animation, when available.
 * @property provider    Source that resolved this artwork:
 *                       `apple_music` | `tidal` | `custom`.
 * @property type        Content type: `video` (animated canvas) or `image` (static only).
 * @property width       Native width of the animation, when known.
 * @property height      Native height of the animation, when known.
 * @property paletteColors ARGB palette extracted from the artwork, if the provider supplies one.
 */
@Serializable
data class CanvasArtwork(
    val id: String? = null,
    val songTitle: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val animated: String? = null,
    val videoUrl: String? = null,
    val staticUrl: String? = null,
    val provider: String? = null,
    val type: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val paletteColors: List<Long> = emptyList(),
) {
    /**
     * The URL the player should use for the animated canvas.
     * Prefers [animated] and falls back to [videoUrl]; `null` when the artwork
     * carries no animation at all (resolver callers treat that as "not found").
     */
    val preferredAnimationUrl: String?
        get() = animated?.takeIf { it.isNotBlank() } ?: videoUrl?.takeIf { it.isNotBlank() }

    /** `true` when this artwork has a usable animation URL. */
    val isAnimated: Boolean
        get() = preferredAnimationUrl != null

    companion object {
        const val PROVIDER_APPLE_MUSIC = "apple_music"
        const val PROVIDER_TIDAL = "tidal"
        const val PROVIDER_CUSTOM = "custom"

        const val TYPE_VIDEO = "video"
        const val TYPE_IMAGE = "image"
    }
}
