package com.skybots.kiko.wake.opensource

import com.skybots.kiko.wake.WakeWordSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSourceWakeConfigTest {
    @Test
    fun defaultsAreConservativeAndLocal() {
        val config = OpenSourceWakeConfig()

        assertTrue(config.sampleRateHz == 16_000)
        assertTrue(config.frameSizeSamples > 0)
        assertTrue(config.threshold == 0.5f)
        assertTrue(config.logMelInferenceStrideFrames >= 1)
        assertTrue(config.modelAssetPath.endsWith("hey_kiko.tflite"))
    }

    @Test
    fun sensitivityMapsToExpectedThresholdOrder() {
        val low = OpenSourceWakeConfig.fromSensitivity(WakeWordSensitivity.LOW)
        val balanced = OpenSourceWakeConfig.fromSensitivity(WakeWordSensitivity.BALANCED)
        val high = OpenSourceWakeConfig.fromSensitivity(WakeWordSensitivity.HIGH)

        assertTrue(low.threshold > balanced.threshold)
        assertTrue(balanced.threshold > high.threshold)
        assertEquals(0.30f, high.threshold, 0.0001f)
    }
}
