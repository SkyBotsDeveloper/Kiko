package com.skybots.kiko.wake

sealed class WakeWordEvent {
    data object Started : WakeWordEvent()
    data object Stopped : WakeWordEvent()
    data object PausedLocked : WakeWordEvent()
    data object WakeDetected : WakeWordEvent()

    data class ScoreDebug(
        val snapshot: WakeScoreSnapshot,
    ) : WakeWordEvent()

    data class Error(
        val message: String,
    ) : WakeWordEvent()
}
