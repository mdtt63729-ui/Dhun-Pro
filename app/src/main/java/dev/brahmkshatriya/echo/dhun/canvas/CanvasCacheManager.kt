package dev.brahmkshatriya.echo.dhun.canvas

import android.content.Context
import dev.brahmkshatriya.echo.dhun.canvas.models.CanvasArtwork

/** Safe no-op disk cache compatibility layer. Playback cache remains handled by Thumbnail.kt. */
object CanvasCacheManager {
    fun init(context: Context) = Unit
    fun getCachedCanvasByUrl(url: String): CanvasArtwork? = null
}
