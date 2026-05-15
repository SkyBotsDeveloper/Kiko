package com.skybots.kiko.wake

import com.skybots.kiko.wake.opensource.WakeEngineHealthStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class WakeNotificationStateMapperTest {
    @Test
    fun activeSafeCalibrationUsesListeningText() {
        val content = WakeNotificationStateMapper.contentFor(
            phrase = "Hey Kiko",
            engineState = WakeWordEngineState.Listening,
            calibrationStatus = WakeCalibrationStatus.OK,
            modelStatus = WakeEngineHealthStatus.LOG_MEL_COMPATIBLE,
        )

        assertEquals("Kiko is listening for Hey Kiko", content.text)
    }

    @Test
    fun unsafeCalibrationUsesTrainingWarningText() {
        val content = WakeNotificationStateMapper.contentFor(
            phrase = "Hey Kiko",
            engineState = WakeWordEngineState.Listening,
            calibrationStatus = WakeCalibrationStatus.NEEDS_BETTER_MODEL,
            modelStatus = WakeEngineHealthStatus.LOG_MEL_COMPATIBLE,
        )

        assertEquals("Kiko wake service is on - model needs better training", content.text)
    }

    @Test
    fun pausedLockedUsesPausedText() {
        val content = WakeNotificationStateMapper.contentFor(
            phrase = "Hey Kiko",
            engineState = WakeWordEngineState.PausedLocked,
            calibrationStatus = WakeCalibrationStatus.OK,
            modelStatus = WakeEngineHealthStatus.LOG_MEL_COMPATIBLE,
        )

        assertEquals("Kiko wake paused while phone is locked", content.text)
    }
}
