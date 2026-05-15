package com.skybots.kiko.wake

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeMicArbitrationTest {
    @Test
    fun manualMicPausesWakeAndResumesWhenEnabled() {
        var enabled = true
        var state = WakeWordEngineState.Listening
        var stopped = false
        var started = false
        val arbitration = WakeMicArbitration(
            isWakeEnabled = { enabled },
            currentWakeState = { state },
            stopWakeService = {
                stopped = true
                state = WakeWordEngineState.Stopped
            },
            startWakeService = {
                started = true
                state = WakeWordEngineState.Starting
            },
        )

        assertTrue(arbitration.pauseForManualMic())
        assertTrue(stopped)
        assertTrue(arbitration.resumeIfNeeded())
        assertTrue(started)
    }

    @Test
    fun manualMicDoesNotPauseWhenWakeIsDisabled() {
        val arbitration = WakeMicArbitration(
            isWakeEnabled = { false },
            currentWakeState = { WakeWordEngineState.Disabled },
            stopWakeService = { error("should not stop") },
            startWakeService = { error("should not start") },
        )

        assertFalse(arbitration.pauseForManualMic())
        assertFalse(arbitration.resumeIfNeeded())
    }

    @Test
    fun wakeDetectedForcesPauseBeforeSpeechRecognizer() {
        var stopped = false
        val arbitration = WakeMicArbitration(
            isWakeEnabled = { true },
            currentWakeState = { WakeWordEngineState.WakeDetected },
            stopWakeService = { stopped = true },
            startWakeService = {},
        )

        assertTrue(arbitration.pauseAfterWakeDetected())
        assertTrue(stopped)
    }
}
