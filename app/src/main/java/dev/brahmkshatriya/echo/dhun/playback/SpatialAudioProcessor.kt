package dev.brahmkshatriya.echo.dhun.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Low-allocation stereo spatializer with a reusable render buffer. It deliberately keeps 2D bit-perfect and
 * applies only bounded gain/pan/delay in the other modes, so mode changes do not require
 * rebuilding ExoPlayer or the AudioTrack.
 */
class SpatialAudioProcessor : AudioProcessor {
    private var inputFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputFormat = AudioProcessor.AudioFormat.NOT_SET
    private var sampleRate = 0
    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID
    private var outputBuffer = AudioProcessor.EMPTY_BUFFER
    private var workBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    private var ended = false
    private var sampleCursor = 0L
    private var delayL = FloatArray(1)
    private var delayR = FloatArray(1)
    private var delayIndex = 0

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.channelCount != 2 ||
            (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT)
        ) {
            inputFormat = inputAudioFormat
            outputFormat = inputAudioFormat
            return inputAudioFormat
        }
        inputFormat = inputAudioFormat
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding
        val delaySamples = (sampleRate * 0.045f).toInt().coerceAtLeast(1)
        delayL = FloatArray(delaySamples)
        delayR = FloatArray(delaySamples)
        delayIndex = 0
        outputFormat = inputAudioFormat
        return outputFormat
    }

    override fun isActive(): Boolean = inputFormat != AudioProcessor.AudioFormat.NOT_SET && channelCount == 2 && (encoding == C.ENCODING_PCM_16BIT || encoding == C.ENCODING_PCM_FLOAT)

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive()) {
            outputBuffer = inputBuffer.slice()
            inputBuffer.position(inputBuffer.limit())
            return
        }
        val mode = SpatialAudioController.currentMode()
        if (mode == SpatialAudioController.Mode.TWO_D) {
            outputBuffer = inputBuffer.slice()
            inputBuffer.position(inputBuffer.limit())
            return
        }

        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val frameSize = channelCount * bytesPerSample
        val inputBytes = inputBuffer.remaining()
        val frames = inputBytes / frameSize
        if (workBuffer.capacity() < inputBytes) {
            workBuffer = ByteBuffer.allocateDirect(inputBytes).order(ByteOrder.nativeOrder())
        } else {
            workBuffer.clear()
        }
        workBuffer.limit(inputBytes)
        val out = workBuffer
        val invSqrt2 = 0.70710677f

        repeat(frames) {
            val left = readSample(inputBuffer)
            val right = readSample(inputBuffer)
            val t = if (sampleRate > 0) sampleCursor.toDouble() / sampleRate else 0.0
            val frequency = when (mode) {
                SpatialAudioController.Mode.THREE_D -> 0.0
                SpatialAudioController.Mode.EIGHT_D -> 0.055
                SpatialAudioController.Mode.SIXTEEN_D -> 0.13
                else -> 0.0
            }
            val pan = if (frequency == 0.0) 0f else sin(2.0 * PI * frequency * t).toFloat()
            val angle = ((pan + 1f) * 0.25f * PI.toFloat()).toFloat()
            val panL = cos(angle) * sqrt(0.5f)
            val panR = sin(angle) * sqrt(0.5f)

            var l: Float
            var r: Float
            when (mode) {
                SpatialAudioController.Mode.THREE_D -> {
                    // Mild mid/side widening, intentionally below clipping.
                    val mid = (left + right) * invSqrt2
                    val side = (left - right) * invSqrt2 * 1.16f
                    l = (mid + side) * invSqrt2
                    r = (mid - side) * invSqrt2
                }
                else -> {
                    val mid = (left + right) * invSqrt2
                    val side = (left - right) * invSqrt2
                    l = mid * panL + side * 0.30f
                    r = mid * panR - side * 0.30f
                }
            }

            // Short feedback delay provides a restrained room tail without relying on
            // device-specific android.media.audiofx implementations.
            val roomMix = when (mode) {
                SpatialAudioController.Mode.THREE_D -> 0.08f
                SpatialAudioController.Mode.EIGHT_D -> 0.13f
                SpatialAudioController.Mode.SIXTEEN_D -> 0.18f
                else -> 0f
            }
            if (roomMix > 0f) {
                val dl = delayL[delayIndex]
                val dr = delayR[delayIndex]
                delayL[delayIndex] = l + dl * 0.22f
                delayR[delayIndex] = r + dr * 0.22f
                l = l * (1f - roomMix) + dr * roomMix
                r = r * (1f - roomMix) + dl * roomMix
                delayIndex = (delayIndex + 1) % delayL.size
            }

            writeSample(out, l.coerceIn(-1f, 1f))
            writeSample(out, r.coerceIn(-1f, 1f))
            sampleCursor++
        }
        out.flip()
        outputBuffer = out
    }

    private fun readSample(buffer: ByteBuffer): Float = if (encoding == C.ENCODING_PCM_FLOAT) {
        buffer.float
    } else {
        buffer.short.toInt() / 32768f
    }

    private fun writeSample(buffer: ByteBuffer, value: Float) {
        if (encoding == C.ENCODING_PCM_FLOAT) buffer.putFloat(value) else buffer.putShort((value * 32767f).toInt().toShort())
    }

    override fun getOutput() = outputBuffer
    override fun isEnded(): Boolean = ended && !outputBuffer.hasRemaining()
    override fun queueEndOfStream() { ended = true }
    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        ended = false
        sampleCursor = 0L
        delayIndex = 0
        delayL.fill(0f)
        delayR.fill(0f)
    }
    override fun reset() {
        flush()
        inputFormat = AudioProcessor.AudioFormat.NOT_SET
        outputFormat = AudioProcessor.AudioFormat.NOT_SET
        sampleRate = 0
        channelCount = 0
        encoding = C.ENCODING_INVALID
        delayL = FloatArray(1)
        delayR = FloatArray(1)
        workBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }
}
