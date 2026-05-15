package com.skybots.kiko.wake

sealed class WakeWordEvent {
    data object Started : WakeWordEvent()
    data object Stopped : WakeWordEvent()
    data object WakeDetected : WakeWordEvent()

    data class Error(
        val message: String,
    ) : WakeWordEvent()
}
