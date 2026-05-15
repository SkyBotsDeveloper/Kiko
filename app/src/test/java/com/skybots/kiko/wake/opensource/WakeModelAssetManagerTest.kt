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
    fun presentModelReportsCompatibilityUnknownBeforeInterpreterLoad() {
        val health = WakeModelAssetManager(modelExists = { true }).health()

        assertEquals(WakeEngineHealthStatus.MODEL_FOUND_COMPATIBILITY_UNKNOWN, health.status)
    }

    @Test
    fun featureInputModelHealthIsNotRunnableUntilAdapterExists() {
        val health = WakeEngineHealth.featureInputNeedsAdapter()

        assertEquals(WakeEngineHealthStatus.FEATURE_INPUT_NEEDS_ADAPTER, health.status)
        assertFalse(health.isReady)
    }

    @Test
    fun logMelModelHealthIsReadyAfterAdapterExists() {
        val health = WakeEngineHealth.logMelCompatible(nMels = 32, frames = 118)

        assertEquals(WakeEngineHealthStatus.LOG_MEL_COMPATIBLE, health.status)
        assertEquals(true, health.isReady)
    }

    @Test
    fun tfliteRunnerClassifiesSupportedShapes() {
        assertEquals(
            WakeModelInputMode.RAW_AUDIO,
            TfliteWakeModelRunner.classifyInputShape(intArrayOf(1, 19_200)),
        )
        assertEquals(
            WakeModelInputMode.LOG_MEL,
            TfliteWakeModelRunner.classifyInputShape(intArrayOf(1, 32, 118, 1)),
        )
        assertEquals(
            WakeModelInputMode.UNSUPPORTED,
            TfliteWakeModelRunner.classifyInputShape(intArrayOf(1, 32, 118)),
        )
    }

    @Test
    fun unsupportedShapeReturnsInvalidHealth() {
        val health = TfliteWakeModelRunner.healthForShapes(
            inputShape = intArrayOf(1, 32, 118),
            outputShape = intArrayOf(1, 1),
        )

        assertEquals(WakeEngineHealthStatus.MODEL_INVALID, health.status)
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
