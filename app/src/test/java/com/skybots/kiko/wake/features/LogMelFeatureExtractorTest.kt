package com.skybots.kiko.wake.features

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class LogMelFeatureExtractorTest {
    @Test
    fun defaultsMatchSanityTrainingReport() {
        val config = LogMelConfig()

        assertEquals(16_000, config.sampleRateHz)
        assertEquals(1_200, config.durationMs)
        assertEquals(32, config.nMels)
        assertEquals(118, config.expectedFrames)
        assertEquals(19_200, config.durationSamples)
        assertEquals(32 * 118, config.outputSize)
    }

    @Test
    fun ringBufferKeepsLatestWindowInOrder() {
        val ring = PcmRingBuffer(capacity = 5)
        ring.append(shortArrayOf(1, 2, 3), 3)
        ring.append(shortArrayOf(4, 5, 6, 7), 4)
        val window = ShortArray(5)

        ring.copyWindow(window)

        assertEquals(listOf<Short>(3, 4, 5, 6, 7), window.toList())
    }

    @Test
    fun melFilterBankHasExpectedDimensions() {
        val config = LogMelConfig()
        val bank = MelFilterBank(config)

        assertEquals(config.nMels, bank.weights.size)
        assertEquals(config.spectrumBins, bank.weights.first().size)
    }

    @Test
    fun featureExtractorWritesExpectedShapeAndRange() {
        val config = LogMelConfig()
        val extractor = LogMelFeatureExtractor(config)
        val pcm = ShortArray(config.durationSamples) { index ->
            (sin(index / 12.0) * 8_000).toInt().toShort()
        }
        val output = FloatArray(config.outputSize)

        extractor.extract(pcm, output)

        assertEquals(32 * 118, output.size)
        assertTrue(output.all { it >= -1f && it <= 1f })
        assertTrue(output.any { it > -1f })
    }
}
