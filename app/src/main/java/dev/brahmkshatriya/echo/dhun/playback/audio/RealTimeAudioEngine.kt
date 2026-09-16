package dev.brahmkshatriya.echo.dhun.playback.audio

import java.util.concurrent.atomic.AtomicReference

/**
 * Reusable PCM DSP engine. UI/player code only selects an AudioMode; this engine owns all render-thread state.
 */
class RealTimeAudioEngine {
    private val presets = PresetManager()
    private val transition = ModeTransitionController(256)
    private val spatial = SpatialProcessor()
    private val delay = DelayProcessor()
    private val reverb = ReverbProcessor()
    private val limiter = Limiter()
    private val requestedMode = AtomicReference(AudioMode.NORMAL_2D)
    private val spatialOut = FloatArray(2)
    private val delayOut = FloatArray(2)
    private val reverbOut = FloatArray(2)
    private var sampleRate = 1
    private var phase = 0f

    fun setMode(mode: AudioMode) {
        requestedMode.set(mode)
        transition.setTarget(mode)
    }

    fun currentTargetMode(): AudioMode = requestedMode.get()

    fun configure(rate: Int) {
        sampleRate = rate.coerceAtLeast(1)
        phase = 0f
        delay.configure(sampleRate)
        reverb.configure(sampleRate)
        transition.reset(requestedMode.get())
    }

    fun processStereo(left: Float, right: Float, out: FloatArray) {
        transition.step()
        val t = transition.amount
        val from = presets.preset(transition.from())
        val to = presets.preset(transition.current())
        val width = lerp(from.width, to.width, t)
        val panDepth = lerp(from.panDepth, to.panDepth, t)
        val motionHz = lerp(from.motionHz, to.motionHz, t) * AudioModeController.movementSpeed()
        val delayMix = lerp(from.delayMix, to.delayMix, t)
        val reverbMix = lerp(from.reverbMix, to.reverbMix, t)
        val wet = lerp(from.wet, to.wet, t)
        val gain = lerp(from.outputGain, to.outputGain, t)

        if (wet <= 0.0001f) {
            out[0] = left
            out[1] = right
            return
        }

        phase += motionHz / sampleRate
        if (phase >= 1f) phase -= 1f
        spatial.process(left, right, width, panDepth, phase, spatialOut)
        var l = left + (spatialOut[0] - left) * wet
        var r = right + (spatialOut[1] - right) * wet
        delay.process(l, r, delayMix, delayOut)
        reverb.process(delayOut[0], delayOut[1], reverbMix, reverbOut)
        limiter.process(reverbOut[0], reverbOut[1], gain, out)
    }

    fun processMono(sample: Float, out: FloatArray) {
        // Mono has no left/right field to widen safely; keep it centered and protected.
        val target = transition.target()
        val gain = presets.preset(target).outputGain
        out[0] = (sample * gain).coerceIn(-0.96f, 0.96f)
    }

    fun reset() {
        phase = 0f
        delay.reset()
        reverb.reset()
        transition.reset(requestedMode.get())
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)
}
