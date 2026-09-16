package dev.brahmkshatriya.echo.dhun.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import dev.brahmkshatriya.echo.dhun.playback.audio.AudioModeController
import dev.brahmkshatriya.echo.dhun.playback.audio.RealTimeAudioEngine
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Media3 adapter for the reusable real-time DSP engine. It never creates a second audio file or restarts playback. */
class SpatialAudioProcessor : AudioProcessor {
    private var inputFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputFormat = AudioProcessor.AudioFormat.NOT_SET
    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID
    private var outputBuffer = AudioProcessor.EMPTY_BUFFER
    private var workBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    private var ended = false
    private val engine = RealTimeAudioEngine()
    private val stereoOut = FloatArray(2)

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        inputFormat = inputAudioFormat
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding
        if ((channelCount != 1 && channelCount != 2) ||
            (encoding != C.ENCODING_PCM_16BIT && encoding != C.ENCODING_PCM_FLOAT)
        ) {
            outputFormat = inputAudioFormat
            return inputAudioFormat
        }
        engine.configure(inputAudioFormat.sampleRate)
        outputFormat = inputAudioFormat
        return outputFormat
    }

    override fun isActive(): Boolean = inputFormat != AudioProcessor.AudioFormat.NOT_SET &&
        (channelCount == 1 || channelCount == 2) &&
        (encoding == C.ENCODING_PCM_16BIT || encoding == C.ENCODING_PCM_FLOAT)

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive()) {
            outputBuffer = inputBuffer.slice()
            inputBuffer.position(inputBuffer.limit())
            return
        }
        engine.setMode(AudioModeController.currentMode())
        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val frameSize = channelCount * bytesPerSample
        val inputBytes = inputBuffer.remaining()
        if (inputBytes == 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }
        if (workBuffer.capacity() < inputBytes) {
            workBuffer = ByteBuffer.allocateDirect(inputBytes).order(ByteOrder.nativeOrder())
        } else {
            workBuffer.clear()
        }
        workBuffer.limit(inputBytes)
        val out = workBuffer
        val frames = inputBytes / frameSize
        repeat(frames) {
            if (channelCount == 1) {
                val sample = readSample(inputBuffer)
                val safe = engine.processMono(sample, stereoOut)
                writeSample(out, safe[0])
            } else {
                val left = readSample(inputBuffer)
                val right = readSample(inputBuffer)
                engine.processStereo(left, right, stereoOut)
                writeSample(out, stereoOut[0])
                writeSample(out, stereoOut[1])
            }
        }
        out.flip()
        outputBuffer = out
    }

    private fun readSample(buffer: ByteBuffer): Float = if (encoding == C.ENCODING_PCM_FLOAT) buffer.float else buffer.short.toInt() / 32768f
    private fun writeSample(buffer: ByteBuffer, value: Float) {
        if (encoding == C.ENCODING_PCM_FLOAT) buffer.putFloat(value.coerceIn(-0.96f, 0.96f))
        else buffer.putShort((value.coerceIn(-0.96f, 0.96f) * 32767f).toInt().toShort())
    }

    override fun getOutput(): ByteBuffer = outputBuffer
    override fun isEnded(): Boolean = ended && !outputBuffer.hasRemaining()
    override fun queueEndOfStream() { ended = true }
    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        ended = false
        engine.reset()
    }
    override fun reset() {
        flush()
        inputFormat = AudioProcessor.AudioFormat.NOT_SET
        outputFormat = AudioProcessor.AudioFormat.NOT_SET
        channelCount = 0
        encoding = C.ENCODING_INVALID
        workBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }
}
