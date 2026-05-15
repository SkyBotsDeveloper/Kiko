package com.skybots.kiko.wake

class WakeMicArbitration(
    private val isWakeEnabled: () -> Boolean,
    private val currentWakeState: () -> WakeWordEngineState,
    private val stopWakeService: () -> Unit,
    private val startWakeService: () -> Unit,
) {
    private var resumePending = false

    fun pauseForManualMic(): Boolean =
        pauseIfNeeded()

    fun pauseAfterWakeDetected(): Boolean =
        pauseIfNeeded(force = true)

    fun resumeIfNeeded(): Boolean {
        val shouldResume = resumePending && isWakeEnabled()
        resumePending = false
        if (shouldResume) {
            startWakeService()
        }
        return shouldResume
    }

    fun clear() {
        resumePending = false
    }

    private fun pauseIfNeeded(force: Boolean = false): Boolean {
        val shouldPause = isWakeEnabled() &&
            (force || currentWakeState() in statesThatMayOwnMic)
        if (shouldPause) {
            resumePending = true
            stopWakeService()
        }
        return shouldPause
    }

    private companion object {
        val statesThatMayOwnMic = setOf(
            WakeWordEngineState.Starting,
            WakeWordEngineState.Listening,
            WakeWordEngineState.WakeDetected,
        )
    }
}
