package com.skybots.kiko.wake.opensource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WakeModelAssetManagerTest {
    @Test
    fun missingModelReturnsMissingHealth() {
        val health = WakeModelAssetManager(modelExists = { false }).health()

        assertEquals(WakeEngineHealthStatus.MODEL_MISSING, health.status)
        assertFalse(health.isReady)
    }

    @Test
    fun tfliteRunnerFailsGracefullyWhenModelMissing() {
        val runner = TfliteWakeModelRunner(
            assetManager = WakeModelAssetManager(modelExists = { false }),
        )

        val health = runner.load()

        assertEquals(WakeEngineHealthStatus.MODEL_MISSING, health.status)
        assertFalse(runner.isReady())
    }
}
