package com.skybots.kiko.wake.features

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin

class LogMelFeatureExtractor(
    private val config: LogMelConfig = LogMelConfig(),
) : WakeFeatureExtractor {
    override val outputSize: Int = config.outputSize

    private val window = WindowFunction.periodicHann(config.winLength)
    private val melFilterBank = MelFilterBank(config)
    private val normalizedPcm = FloatArray(config.durationSamples)
    private val powerSpectrum = FloatArray(config.spectrumBins)
    private val melPower = FloatArray(config.outputSize)
    private val cosineTable = Array(config.spectrumBins) { bin ->
        FloatArray(config.nFft) { index ->
            cos((2.0 * PI * bin * index) / config.nFft).toFloat()
        }
    }
    private val sineTable = Array(config.spectrumBins) { bin ->
        FloatArray(config.nFft) { index ->
            sin((2.0 * PI * bin * index) / config.nFft).toFloat()
        }
    }

    override fun extract(
        pcmWindow: ShortArray,
        output: FloatArray,
    ) {
        require(pcmWindow.size >= config.durationSamples) {
            "PCM window must contain at least ${config.durationSamples} samples."
        }
        require(output.size >= outputSize) {
            "Output buffer must contain at least $outputSize floats."
        }

        normalizePcm(pcmWindow)
        computeMelPower()
        normalizeLogMel(output)
    }

    private fun normalizePcm(pcmWindow: ShortArray) {
        var peak = 0f
        for (index in 0 until config.durationSamples) {
            val value = pcmWindow[index] / PCM_16_SCALE
            normalizedPcm[index] = value
            val absValue = kotlin.math.abs(value)
            if (absValue > peak) peak = absValue
        }
        if (peak > 0f) {
            for (index in normalizedPcm.indices) {
                normalizedPcm[index] /= peak
            }
        }
    }

    private fun computeMelPower() {
        melPower.fill(0f)
        for (frame in 0 until config.expectedFrames) {
            val frameStart = frame * config.hopLength
            computePowerSpectrum(frameStart)
            for (mel in 0 until config.nMels) {
                val weights = melFilterBank.weights[mel]
                var sum = 0.0
                for (bin in 0 until config.spectrumBins) {
                    sum += powerSpectrum[bin] * weights[bin]
                }
                melPower[(mel * config.expectedFrames) + frame] = sum.toFloat().coerceAtLeast(0f)
            }
        }
    }

    private fun computePowerSpectrum(frameStart: Int) {
        for (bin in 0 until config.spectrumBins) {
            val cos = cosineTable[bin]
            val sin = sineTable[bin]
            var real = 0.0
            var imaginary = 0.0
            for (sampleIndex in 0 until config.nFft) {
                val windowIndex = sampleIndex.coerceAtMost(config.winLength - 1)
                val sample = normalizedPcm[frameStart + sampleIndex] * window[windowIndex]
                real += sample * cos[sampleIndex]
                imaginary -= sample * sin[sampleIndex]
            }
            powerSpectrum[bin] = ((real * real) + (imaginary * imaginary)).toFloat()
        }
    }

    private fun normalizeLogMel(output: FloatArray) {
        var maxPower = AMIN
        for (value in melPower) {
            if (value > maxPower) maxPower = value
        }
        val referenceDb = 10f * log10(max(maxPower, AMIN))

        for (index in 0 until outputSize) {
            val db = (10f * log10(max(melPower[index], AMIN))) - referenceDb
            val clipped = db.coerceAtLeast(-TOP_DB)
            output[index] = ((((clipped + TOP_DB) / TOP_DB) * 2f) - 1f).coerceIn(-1f, 1f)
        }
    }

    private companion object {
        const val PCM_16_SCALE = 32768f
        const val AMIN = 1e-10f
        const val TOP_DB = 80f
    }
}
