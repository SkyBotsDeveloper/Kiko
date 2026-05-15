package com.skybots.kiko.wake.features

import kotlin.math.ln
import kotlin.math.max

class MelFilterBank(
    private val config: LogMelConfig,
) {
    val weights: Array<FloatArray> = buildWeights()

    private fun buildWeights(): Array<FloatArray> {
        val fftFrequencies = FloatArray(config.spectrumBins) { bin ->
            (bin * config.sampleRateHz.toFloat()) / config.nFft
        }
        val melMin = hzToMel(config.fMinHz)
        val melMax = hzToMel(config.fMaxHz)
        val melPoints = FloatArray(config.nMels + 2) { index ->
            melMin + ((melMax - melMin) * index / (config.nMels + 1))
        }
        val hzPoints = FloatArray(melPoints.size) { index -> melToHz(melPoints[index]) }
        val bank = Array(config.nMels) { FloatArray(config.spectrumBins) }

        for (melIndex in 0 until config.nMels) {
            val lower = hzPoints[melIndex]
            val center = hzPoints[melIndex + 1]
            val upper = hzPoints[melIndex + 2]
            val lowerWidth = max(center - lower, MIN_WIDTH_HZ)
            val upperWidth = max(upper - center, MIN_WIDTH_HZ)
            val enorm = 2f / max(upper - lower, MIN_WIDTH_HZ)

            for (bin in fftFrequencies.indices) {
                val frequency = fftFrequencies[bin]
                val lowerSlope = (frequency - lower) / lowerWidth
                val upperSlope = (upper - frequency) / upperWidth
                val value = minOf(lowerSlope, upperSlope).coerceAtLeast(0f)
                bank[melIndex][bin] = value * enorm
            }
        }
        return bank
    }

    private fun hzToMel(frequency: Float): Float =
        if (frequency < MIN_LOG_HZ) {
            frequency / F_SP
        } else {
            MIN_LOG_MEL + (ln(frequency / MIN_LOG_HZ) / LOG_STEP).toFloat()
        }

    private fun melToHz(mel: Float): Float =
        if (mel < MIN_LOG_MEL) {
            mel * F_SP
        } else {
            (MIN_LOG_HZ * kotlin.math.exp(LOG_STEP * (mel - MIN_LOG_MEL))).toFloat()
        }

    private companion object {
        const val F_SP = 200f / 3f
        const val MIN_LOG_HZ = 1_000f
        const val MIN_LOG_MEL = MIN_LOG_HZ / F_SP
        val LOG_STEP: Float = (ln(6.4) / 27.0).toFloat()
        const val MIN_WIDTH_HZ = 1e-6f
    }
}
