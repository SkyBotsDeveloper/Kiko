package com.skybots.kiko.assistant

import com.skybots.kiko.assistant.parser.AssistantIntent

data class AssistantResult(
    val intent: AssistantIntent,
    val response: String,
    val runtimeState: AssistantRuntimeState = AssistantRuntimeState.IDLE,
    val errorMessage: String? = null,
)
