package dev.brahmkshatriya.echo.utils.image

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import coil3.Image
import coil3.asDrawable
import coil3.imageLoader
import coil3.load
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.error
import coil3.request.placeholder
import coil3.request.target
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import coil3.transform.Transformation
import dev.brahmkshatriya.echo.common.models.ImageHolder

object ImageUtils {

    private fun <T> tryWith(print: Boolean = false, block: () -> T): T? {
        return try {
            block()
        } catch (e: Throwable) {
            if (print) e.printStackTrace()
            null
        }
    }

    private suspend fun <T> tryWithSuspend(print: Boolean = true, block: suspend () -> T): T? {
        return try {
            block()
        } catch (e: Throwable) {
            if (print) e.printStackTrace()
            null
        }
    }

    private fun View.enqueue(builder: ImageRequest.Builder) =
        context.imageLoader.enqueue(builder.build())

    fun ImageHolder?.loadInto(
        imageView: ImageView, placeholder: Int? = null, errorDrawable: Int? = null
    ) = tryWith {
        // Safe for hardware bitmaps: the result is only ever drawn into the view.
        val request = createRequest(
            imageView.context, placeholder, errorDrawable, allowHardware = true
        )
        request.target(imageView)
        imageView.enqueue(request)
    }

    fun ImageHolder.getCachedDrawable(context: Context): Drawable? {
        val key = diskId ?: return null
        return context.imageLoader.diskCache?.openSnapshot(key)?.use {
            Drawable.createFromPath(it.data.toFile().absolutePath)
        }
    }

    fun <T : View> ImageHolder?.loadWithThumb(
        view: T, thumbnail: Drawable? = null,
        error: Int? = null, onDrawable: T.(Drawable?) -> Unit
    ) = tryWith(true) {
        tryWith(false) { onDrawable(view, thumbnail) }
        val request = createRequest(view.context, null, error)
        fun setDrawable(image: Image?) {
            val drawable = image?.asDrawable(view.resources)
            tryWith(false) { onDrawable(view, drawable) }
        }
        request.target({}, ::setDrawable, ::setDrawable)
        view.enqueue(request)
    }

    private val circleCrop = CircleCropTransformation()
    private val squareCrop = SquareCropTransformation()
    fun <T : View> ImageHolder?.loadAsCircle(
        view: T,
        placeholder: Int? = null,
        error: Int? = null,
        onDrawable: (Drawable?) -> Unit
    ) = tryWith {
        val request = createRequest(view.context, placeholder, error, circleCrop)
        fun setDrawable(image: Image?) {
            val drawable = image?.asDrawable(view.resources)
            tryWith(false) { onDrawable(drawable) }
        }
        request.target(::setDrawable, ::setDrawable, ::setDrawable)
        view.enqueue(request)
    }

    suspend fun ImageHolder?.loadDrawable(
        context: Context
    ) = tryWithSuspend {
        val request = createRequest(context, null, null)
        context.imageLoader.execute(request.build()).image?.asDrawable(context.resources)
    }

    suspend fun ImageHolder?.loadAsCircleDrawable(
        context: Context
    ) = tryWithSuspend {
        val request = createRequest(context, null, null, circleCrop)
        context.imageLoader.execute(request.build()).image?.asDrawable(context.resources)
    }

    fun ImageView.loadBlurred(drawable: Drawable?, radius: Float) = tryWith {
        if (drawable == null) setImageDrawable(null)
        load(drawable) {
            transformations(BlurTransformation(context, radius))
        }
    }

    /**
     * Image hosts that accept a `=wN-hN` / `=sN` size parameter on the URL.
     * Rewriting it is how a bigger source image is requested.
     */
    private val resizableImageHosts =
        listOf("googleusercontent.com", "ggpht.com", "ytimg.com")

    /** The size token of a Google image URL, e.g. `=w120-h120` or `=s160`. */
    private val sizeToken = Regex("""(?<==)(?:w(\d+)-h(\d+)|s(\d+)|w(\d+)|h(\d+))(?=[-&]|$)""")

    /** Small YouTube stills, upgraded to a bigger one that is always available. */
    private val smallYouTubeStills =
        Regex("""/(vi|vi_webp)/([^/]+)/(?:default|mqdefault)\.(?:jpg|webp)""")

    /**
     * Requested thumbnail resolution. Deliberately well above Echo Music's
     * `resize(544, 544)` so even the full-screen player cover stays sharp; Coil
     * still downsamples to whatever the view actually needs.
     */
    private const val THUMBNAIL_SIZE = 1000

    /**
     * Ask the image host for a bigger source image. Without this the extension's small
     * thumbnail URL is what gets decoded and every cover — including the full-screen
     * player — looks soft.
     */
    private fun String.upscaleThumbnail(size: Int = THUMBNAIL_SIZE): String {
        if (resizableImageHosts.none { contains(it, ignoreCase = true) }) return this
        sizeToken.find(this)?.let { return replaceRange(it.range, "w$size-h$size") }
        return replace(smallYouTubeStills, "/\$1/\$2/hqdefault.jpg")
    }

    /** Bump to invalidate previously cached (lower resolution) images. */
    private const val IMAGE_CACHE_VERSION = 2

    private val ImageHolder.diskId
        get() = when (this) {
            is ImageHolder.NetworkRequestImageHolder ->
                "${request.toString().hashCode()}-v$IMAGE_CACHE_VERSION"

            else -> null
        }

    private fun createRequest(
        imageHolder: ImageHolder,
        builder: ImageRequest.Builder,
    ) = imageHolder.run {
        builder.diskCacheKey(diskId)
        when (this) {
            is ImageHolder.ResourceUriImageHolder -> builder.data(uri)
            is ImageHolder.NetworkRequestImageHolder -> {
                val headerBuilder = NetworkHeaders.Builder()
                request.headers.forEach { (key, value) ->
                    headerBuilder[key] = value
                }
                builder.httpHeaders(headerBuilder.build())
                builder.data(request.url.upscaleThumbnail())
            }

            is ImageHolder.ResourceIdImageHolder -> builder.data(resId)
            is ImageHolder.HexColorImageHolder -> builder.data(hex.toColorInt().toDrawable())
        }
    }

    private fun ImageHolder?.createRequest(
        context: Context,
        placeholder: Int?,
        errorDrawable: Int?,
        vararg transformations: Transformation,
        allowHardware: Boolean = false
    ): ImageRequest.Builder {
        val builder = ImageRequest.Builder(context)
        var error = errorDrawable
        if (error == null) error = placeholder

        if (this == null) {
            if (error != null) builder.data(error)
            return builder
        }
        createRequest(this, builder)
        placeholder?.let { builder.placeholder(it) }
        error?.let { builder.error(it) }
        val list = if (crop) listOf(squareCrop, *transformations) else transformations.toList()
        if (list.isNotEmpty()) builder.transformations(list)
        // Hardware bitmaps are far cheaper to draw, but they cannot be transformed
        // or read back (toBitmap()), so only allow them when neither is needed.
        builder.allowHardware(allowHardware && list.isEmpty())
        return builder
    }
}