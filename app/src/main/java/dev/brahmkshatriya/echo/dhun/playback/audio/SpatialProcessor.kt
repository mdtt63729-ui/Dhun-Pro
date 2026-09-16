package dev.brahmkshatriya.echo.dhun.playback.audio

class SpatialProcessor {
    private val stereo = StereoProcessor()
    private val panner = PanningProcessor()
    private val stereoOut = FloatArray(2)

    fun process(left: Float, right: Float, width: Float, panDepth: Float, phase: Float, out: FloatArray) {
        stereo.process(left, right, width, stereoOut)
        panner.process(stereoOut[0], stereoOut[1], phase, panDepth, out)
    }
}
