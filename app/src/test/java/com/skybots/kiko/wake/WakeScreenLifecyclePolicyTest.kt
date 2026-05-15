package com.skybots.kiko.wake

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeScreenLifecyclePolicyTest {
    private val policy = WakeScreenLifecyclePolicy()

    @Test
    fun screenOffPausesActiveWakeButKeepsPreferenceEnabledExternally() {
        val decision = policy.onScreenOff(
            wakeEnabled = true,
            currentState = WakeWordEngineState.Listening,
        )

        assertTrue(decision.shouldPause)
        assertFalse(decision.shouldResume)
    }

    @Test
    fun userPresentResumesOnlyWhenWakeWasEnabledAndPaused() {
        assertTrue(
            policy.onUserPresent(
                wakeEnabled = true,
                currentState = WakeWordEngineState.PausedLocked,
            ).shouldResume,
        )
        assertFalse(
            policy.onUserPresent(
                wakeEnabled = false,
                currentState = WakeWordEngineState.PausedLocked,
            ).shouldResume,
        )
    }

    @Test
    fun manuallyDisabledWakeDoesNotPauseOrResume() {
        assertFalse(
            policy.onScreenOff(
                wakeEnabled = false,
                currentState = WakeWordEngineState.Listening,
            ).shouldPause,
        )
        assertFalse(
            policy.onUserPresent(
                wakeEnabled = false,
                currentState = WakeWordEngineState.PausedLocked,
            ).shouldResume,
        )
    }
}
