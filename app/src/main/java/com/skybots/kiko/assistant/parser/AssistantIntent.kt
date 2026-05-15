package com.skybots.kiko.assistant.parser

import com.skybots.kiko.assistant.language.LanguageHint

data class AssistantIntent(
    val type: IntentType,
    val rawText: String,
    val target: String? = null,
    val value: String? = null,
    val languageHint: LanguageHint = LanguageHint.SYSTEM_DEFAULT,
)
