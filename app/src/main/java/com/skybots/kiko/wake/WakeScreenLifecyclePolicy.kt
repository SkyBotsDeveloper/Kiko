package com.skybots.kiko.wake

data class WakeScreenLifecycleDecision(
    val shouldPause: Boolean = false,
    val shouldResume: Boolean = false,
)

class WakeScreenLifecyclePolicy {
    fun onScreenOff(
        wakeEnabled: Boolean,
        currentState: WakeWordEngineState,
    ): WakeScreenLifecycleDecision =
        WakeScreenLifecycleDecision(
            shouldPause = wakeEnabled && currentState in activeStates,
        )

    fun onUserPresent(
        wakeEnabled: Boolean,
        currentState: WakeWordEngineState,
    ): WakeScreenLifecycleDecision =
        WakeScreenLifecycleDecision(
            shouldResume = wakeEnabled && currentState == WakeWordEngineState.PausedLocked,
        )

    private companion object {
        val activeStates = setOf(
            WakeWordEngineState.Starting,
            WakeWordEngineState.Listening,
            WakeWordEngineState.WakeDetected,
            WakeWordEngineState.Error,
        )
    }
}
