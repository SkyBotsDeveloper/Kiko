package com.skybots.kiko.wake.features

data class LogMelConfig(
    val sampleRateHz: Int = 16_000,
    val durationMs: Int = 1_200,
    val nMels: Int = 32,
    val expectedFrames: Int = 118,
    val nFft: Int = 400,
    val hopLength: Int = 160,
    val winLength: Int = 400,
    val fMinHz: Float = 20f,
    val fMaxHz: Float = 7_600f,
) {
    val durationSamples: Int = sampleRateHz * durationMs / 1_000
    val spectrumBins: Int = (nFft / 2) + 1
    val outputSize: Int = nMels * expectedFrames

    companion object {
        fun fromModelShape(shape: IntArray): LogMelConfig? {
            if (shape.size != 4 || shape[0] != 1 || shape[3] != 1) return null
            val nMels = shape[1]
            val frames = shape[2]
            if (nMels <= 0 || frames <= 0) return null
            val durationMs = when (frames) {
                118 -> 1_200
                138 -> 1_400
                else -> {
                    val minimumSamples = 400 + ((frames - 1) * 160)
                    ((minimumSamples * 1_000) / 16_000)
                }
            }
            return LogMelConfig(
                durationMs = durationMs,
                nMels = nMels,
                expectedFrames = frames,
            )
        }
    }
}
