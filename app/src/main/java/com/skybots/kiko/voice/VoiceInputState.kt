package com.skybots.kiko.voice

sealed interface VoiceInputState {
    data object Idle : VoiceInputState
    data object Listening : VoiceInputState
    data object Processing : VoiceInputState
    data class Error(val message: String) : VoiceInputState
}
