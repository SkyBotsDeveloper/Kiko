package com.skybots.kiko.orbit

import com.skybots.kiko.wake.WakeCalibrationStatus
import com.skybots.kiko.wake.WakeScoreSnapshot
import com.skybots.kiko.wake.WakeWordEngineState
import org.junit.Assert.assertEquals
import org.junit.Test

class OrbitUiStateMapperTest {
    @Test
    fun tapBubbleCanOpenPanelState() {
        val state = OrbitUiStateMapper.fromWakeState(
            wakeWordState = WakeWordEngineState.Listening,
            wakeScoreSnapshot = WakeScoreSnapshot(calibrationStatus = WakeCalibrationStatus.OK),
            panelVisible = true,
        )

        assertEquals(OrbitOverlayMode.Panel, state.mode)
        assertEquals(OrbitOverlayVisualState.WakeListening, state.visualState)
    }

    @Test
    fun closeReturnsToBubbleState() {
        val state = OrbitUiStateMapper.fromWakeState(
            wakeWordState = WakeWordEngineState.Listening,
            wakeScoreSnapshot = WakeScoreSnapshot(calibrationStatus = WakeCalibrationStatus.OK),
            panelVisible = false,
        )

        assertEquals(OrbitOverlayMode.Bubble, state.mode)
    }

    @Test
    fun unsafeModelShowsBlockedState() {
        val state = OrbitUiStateMapper.fromWakeState(
            wakeWordState = WakeWordEngineState.Listening,
            wakeScoreSnapshot = WakeScoreSnapshot(calibrationStatus = WakeCalibrationStatus.NEEDS_BETTER_MODEL),
            panelVisible = true,
        )

        assertEquals(OrbitOverlayVisualState.UnsafeModel, state.visualState)
        assertEquals("Wake blocked", state.title)
    }

    @Test
    fun lockPauseDimsOverlay() {
        val state = OrbitUiStateMapper.fromWakeState(
            wakeWordState = WakeWordEngineState.PausedLocked,
            wakeScoreSnapshot = WakeScoreSnapshot(calibrationStatus = WakeCalibrationStatus.OK),
        )

        assertEquals(OrbitOverlayVisualState.PausedLocked, state.visualState)
    }
}
