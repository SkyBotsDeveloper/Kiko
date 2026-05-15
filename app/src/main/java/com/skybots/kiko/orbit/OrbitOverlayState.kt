package com.skybots.kiko.orbit

import com.skybots.kiko.wake.WakeCalibrationStatus
import com.skybots.kiko.wake.WakeScoreSnapshot
import com.skybots.kiko.wake.WakeWordEngineState

enum class OrbitOverlayMode {
    Bubble,
    Panel,
}

enum class OrbitOverlayVisualState {
    Idle,
    WakeListening,
    Listening,
    Processing,
    Speaking,
    UnsafeModel,
    PausedLocked,
    Error,
}

data class OrbitOverlayState(
    val mode: OrbitOverlayMode = OrbitOverlayMode.Bubble,
    val visualState: OrbitOverlayVisualState = OrbitOverlayVisualState.Idle,
    val title: String = "Kiko",
    val message: String = "Tap to talk",
)

object OrbitUiStateMapper {
    fun fromWakeState(
        wakeWordState: WakeWordEngineState,
        wakeScoreSnapshot: WakeScoreSnapshot,
        panelVisible: Boolean = false,
    ): OrbitOverlayState {
        val unsafeCalibration = wakeScoreSnapshot.calibrationStatus == WakeCalibrationStatus.NEEDS_BETTER_MODEL ||
            wakeScoreSnapshot.calibrationStatus == WakeCalibrationStatus.UNSAFE_BASELINE
        return when {
            wakeWordState == WakeWordEngineState.PausedLocked -> OrbitOverlayState(
                mode = if (panelVisible) OrbitOverlayMode.Panel else OrbitOverlayMode.Bubble,
                visualState = OrbitOverlayVisualState.PausedLocked,
                title = "Wake paused",
                message = "Kiko wake paused while phone is locked.",
            )
            unsafeCalibration -> OrbitOverlayState(
                mode = if (panelVisible) OrbitOverlayMode.Panel else OrbitOverlayMode.Bubble,
                visualState = OrbitOverlayVisualState.UnsafeModel,
                title = "Wake blocked",
                message = "Train a better model for real Hey Kiko detection.",
            )
            wakeWordState == WakeWordEngineState.Listening -> OrbitOverlayState(
                mode = if (panelVisible) OrbitOverlayMode.Panel else OrbitOverlayMode.Bubble,
                visualState = OrbitOverlayVisualState.WakeListening,
                title = "Hey Kiko listening",
                message = "Foreground wake service is active.",
            )
            wakeWordState == WakeWordEngineState.Error -> OrbitOverlayState(
                mode = if (panelVisible) OrbitOverlayMode.Panel else OrbitOverlayMode.Bubble,
                visualState = OrbitOverlayVisualState.Error,
                title = "Wake error",
                message = "Open settings to inspect wake status.",
            )
            else -> OrbitOverlayState(
                mode = if (panelVisible) OrbitOverlayMode.Panel else OrbitOverlayMode.Bubble,
                visualState = OrbitOverlayVisualState.Idle,
                title = "Kiko",
                message = "Tap to talk.",
            )
        }
    }

    fun manualListening(): OrbitOverlayState =
        OrbitOverlayState(
            mode = OrbitOverlayMode.Panel,
            visualState = OrbitOverlayVisualState.Listening,
            title = "Listening...",
            message = "Kiko is listening.",
        )
}
