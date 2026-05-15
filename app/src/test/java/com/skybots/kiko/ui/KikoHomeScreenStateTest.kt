package com.skybots.kiko.ui

import com.skybots.kiko.assistant.AssistantRuntimeState
import com.skybots.kiko.wake.WakeCalibrationStatus
import com.skybots.kiko.wake.WakeScoreSnapshot
import com.skybots.kiko.wake.WakeWordEngineState
import com.skybots.kiko.wake.opensource.WakeEngineHealth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KikoHomeScreenStateTest {
    @Test
    fun listeningAndProcessingShowOrbit() {
        assertTrue(shouldShowListeningOrbit(AssistantRuntimeState.LISTENING))
        assertTrue(shouldShowListeningOrbit(AssistantRuntimeState.PROCESSING))
    }

    @Test
    fun idleAndSpeakingDoNotShowOrbit() {
        assertFalse(shouldShowListeningOrbit(AssistantRuntimeState.IDLE))
        assertFalse(shouldShowListeningOrbit(AssistantRuntimeState.SPEAKING))
    }

    @Test
    fun manualMicStateUsesListeningOrbitVisual() {
        assertEquals(
            OrbitVisualState.LISTENING,
            orbitVisualState(
                runtimeState = AssistantRuntimeState.LISTENING,
                wakeWordState = WakeWordEngineState.Listening,
                calibrationStatus = WakeCalibrationStatus.OK,
            ),
        )
    }

    @Test
    fun unsafeCalibrationShowsNeedsModelStateAndBlockedText() {
        val snapshot = WakeScoreSnapshot(
            calibrationStatus = WakeCalibrationStatus.NEEDS_BETTER_MODEL,
            baselineScore = 0.501f,
        )

        assertEquals(
            OrbitVisualState.NEEDS_MODEL,
            orbitVisualState(
                runtimeState = AssistantRuntimeState.IDLE,
                wakeWordState = WakeWordEngineState.Listening,
                calibrationStatus = snapshot.calibrationStatus,
            ),
        )
        assertEquals(
            "Wake listening, model needs better training",
            orbitStatusText(
                wakeWordState = WakeWordEngineState.Listening,
                wakeModelHealth = WakeEngineHealth.logMelCompatible(32, 118),
                wakeScoreSnapshot = snapshot,
                runtimeState = AssistantRuntimeState.IDLE,
            ),
        )
    }

    @Test
    fun pausedLockStateShowsPausedOrbitText() {
        assertEquals(
            OrbitVisualState.PAUSED,
            orbitVisualState(
                runtimeState = AssistantRuntimeState.IDLE,
                wakeWordState = WakeWordEngineState.PausedLocked,
                calibrationStatus = WakeCalibrationStatus.OK,
            ),
        )
        assertEquals(
            "Wake paused while phone is locked",
            orbitStatusText(
                wakeWordState = WakeWordEngineState.PausedLocked,
                wakeModelHealth = WakeEngineHealth.logMelCompatible(32, 118),
                wakeScoreSnapshot = WakeScoreSnapshot(calibrationStatus = WakeCalibrationStatus.OK),
                runtimeState = AssistantRuntimeState.IDLE,
            ),
        )
    }
}
