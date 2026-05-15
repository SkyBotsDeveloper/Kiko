package com.skybots.kiko.ui

import com.skybots.kiko.assistant.AssistantRuntimeState

data class KikoHomeUiState(
    val transcript: String = "Your voice input will appear here.",
    val kikoResponse: String = "Tap the mic to try a local command.",
    val runtimeState: AssistantRuntimeState = AssistantRuntimeState.IDLE,
    val statusMessage: String = "Ready for manual voice input.",
)
