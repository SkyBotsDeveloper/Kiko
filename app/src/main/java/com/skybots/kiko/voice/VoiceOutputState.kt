package com.skybots.kiko.voice

sealed interface VoiceOutputState {
    data object Idle : VoiceOutputState
    data object Initializing : VoiceOutputState
    data object Speaking : VoiceOutputState
    data class Error(val message: String) : VoiceOutputState
}
