package com.skybots.kiko.wake

import com.skybots.kiko.wake.opensource.WakeEngineHealthStatus

data class WakeNotificationContent(
    val title: String,
    val text: String,
)

object WakeNotificationStateMapper {
    fun contentFor(
        phrase: String,
        engineState: WakeWordEngineState,
        calibrationStatus: WakeCalibrationStatus,
        modelStatus: WakeEngineHealthStatus,
        floatingOrbitActive: Boolean = false,
    ): WakeNotificationContent =
        when {
            floatingOrbitActive -> WakeNotificationContent(
                title = "Kiko Orbit",
                text = "Kiko floating orbit is active",
            )
            engineState == WakeWordEngineState.PausedLocked -> WakeNotificationContent(
                title = "Kiko Wake Word",
                text = "Kiko wake paused while phone is locked",
            )
            calibrationStatus == WakeCalibrationStatus.NEEDS_BETTER_MODEL ||
                calibrationStatus == WakeCalibrationStatus.UNSAFE_BASELINE -> WakeNotificationContent(
                title = "Kiko Wake Word",
                text = "Kiko wake service is on - model needs better training",
            )
            modelStatus == WakeEngineHealthStatus.LOG_MEL_COMPATIBLE ||
                modelStatus == WakeEngineHealthStatus.RAW_AUDIO_COMPATIBLE ||
                modelStatus == WakeEngineHealthStatus.READY -> WakeNotificationContent(
                title = "Kiko Wake Word",
                text = "Kiko is listening for $phrase",
            )
            else -> WakeNotificationContent(
                title = "Kiko Wake Word",
                text = "Kiko wake service is on",
            )
        }
}
