package com.skybots.kiko.wake.features

interface WakeFeatureExtractor {
    val outputSize: Int

    fun extract(
        pcmWindow: ShortArray,
        output: FloatArray,
    )
}
