package dev.brahmkshatriya.echo.playback.renderer

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * Playback renderers used by the legacy Echo player.
 *
 * Keep the audio path as close to Media3 defaults as possible. The previous
 * implementation injected a custom SilenceSkippingAudioProcessor + SonicAudioProcessor
 * chain for every device. That made playback dependent on device-specific AudioTrack
 * implementations and was a common source of native playback failures.
 */
@UnstableApi
class RenderersFactory(context: Context) : DefaultRenderersFactory(context) {

    init {
        // If the preferred decoder fails, Media3 may fall back to another compatible
        // decoder instead of terminating playback.
        setEnableDecoderFallback(true)
    }

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): DefaultAudioSink =
        DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .build()
}
