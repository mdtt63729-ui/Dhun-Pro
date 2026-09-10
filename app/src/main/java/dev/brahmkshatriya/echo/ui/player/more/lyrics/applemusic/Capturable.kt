package dev.brahmkshatriya.echo.ui.player.more.lyrics.applemusic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Draws a Composable into an [ImageBitmap] on demand.
 *
 * Ported from Capturable 3.0.1 (MIT, Copyright (c) 2022 Shreyas Patil,
 * https://github.com/PatilShreyas/Capturable) rather than taken as a dependency.
 * Uses only `androidx.compose.ui.graphics.layer`, which echo already includes via compose-bom.
 */
class CaptureController internal constructor(
    internal val graphicsLayer: GraphicsLayer,
) {
    /**
     * A Channel, not a SharedFlow. A capture asked for while composition is still settling
     * is emitted BEFORE the modifier node attaches and starts collecting. A hot SharedFlow
     * drops that emission on the floor and the caller's Deferred never completes. An UNLIMITED
     * Channel buffers it until the node is there to take it.
     */
    private val requests = Channel<CaptureRequest>(capacity = Channel.UNLIMITED)
    internal val captureRequests = requests.receiveAsFlow()

    /**
     * Requests a capture and returns the bitmap asynchronously. Safe to call from the main
     * thread, but call it from a callback — never from composition itself.
     */
    fun captureAsync(): Deferred<ImageBitmap> {
        val deferred = CompletableDeferred<ImageBitmap>()
        requests.trySend(CaptureRequest(deferred))
        return deferred
    }

    internal class CaptureRequest(
        val imageBitmapDeferred: CompletableDeferred<ImageBitmap>,
    )
}

/** Creates a [CaptureController] bound to a remembered [GraphicsLayer]. */
@Composable
fun rememberCaptureController(): CaptureController {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(graphicsLayer) { CaptureController(graphicsLayer) }
}

/**
 * Records this Composable's drawing into [controller]'s layer, so it can be read back as a
 * bitmap. The content still draws normally — the layer is recorded and then drawn straight
 * onto the visible canvas, so applying this changes nothing about how the node looks.
 */
fun Modifier.capturable(controller: CaptureController): Modifier =
    this then CapturableModifierNodeElement(controller)

private data class CapturableModifierNodeElement(
    private val controller: CaptureController,
) : ModifierNodeElement<CapturableModifierNode>() {
    override fun create(): CapturableModifierNode = CapturableModifierNode(controller)

    override fun update(node: CapturableModifierNode) {
        node.updateController(controller)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "capturable"
        properties["controller"] = controller
    }
}

private class CapturableModifierNode(
    controller: CaptureController,
) : Modifier.Node(),
    DrawModifierNode {

    private val currentController = MutableStateFlow(controller)

    private val currentGraphicsLayer get() = currentController.value.graphicsLayer

    override fun onAttach() {
        super.onAttach()
        coroutineScope.launch { serveCaptureRequests() }
    }

    fun updateController(newController: CaptureController) {
        currentController.value = newController
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun serveCaptureRequests() {
        currentController
            .flatMapLatest { it.captureRequests }
            .collect { request ->
                val deferred = request.imageBitmapDeferred
                try {
                    deferred.complete(currentGraphicsLayer.toImageBitmap())
                } catch (error: Throwable) {
                    deferred.completeExceptionally(error)
                }
            }
    }

    override fun ContentDrawScope.draw() {
        currentGraphicsLayer.record { this@draw.drawContent() }
        drawLayer(currentGraphicsLayer)
    }
}
