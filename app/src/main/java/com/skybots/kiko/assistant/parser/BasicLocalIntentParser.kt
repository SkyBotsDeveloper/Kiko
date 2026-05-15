package com.skybots.kiko.assistant.parser

import com.skybots.kiko.assistant.language.LanguageHint
import java.util.Locale

class BasicLocalIntentParser : IntentParser {
    override fun parse(text: String): AssistantIntent {
        val normalized = text.trim().lowercase(Locale.ROOT)
        val languageHint = detectLanguageHint(normalized)

        if (normalized.isBlank()) {
            return AssistantIntent(
                type = IntentType.UNKNOWN,
                rawText = text,
                languageHint = languageHint,
            )
        }

        val type = when {
            isCreatorQuestion(normalized) -> IntentType.CREATOR_IDENTITY
            normalized.contains("open") -> IntentType.OPEN_APP
            normalized.contains("call") -> IntentType.CALL_CONTACT
            normalized.contains("flashlight") || normalized.contains("torch") -> {
                if (normalized.contains("off") || normalized.contains("band")) {
                    IntentType.FLASHLIGHT_OFF
                } else {
                    IntentType.FLASHLIGHT_ON
                }
            }
            normalized.contains("volume") -> IntentType.SET_VOLUME
            normalized.contains("brightness") -> IntentType.SET_BRIGHTNESS
            normalized.contains("alarm") -> IntentType.SET_ALARM
            normalized.contains("reminder") -> IntentType.SET_REMINDER
            normalized.contains("internet") ||
                normalized.contains("search") ||
                normalized.contains("google") -> IntentType.INTERNET_REQUIRED_QUERY
            else -> IntentType.UNKNOWN
        }

        return AssistantIntent(
            type = type,
            rawText = text,
            languageHint = languageHint,
        )
    }

    private fun isCreatorQuestion(text: String): Boolean =
        text.contains("who created you") ||
            text.contains("who made you") ||
            text.contains("tumhe kisne banaya") ||
            text.contains("kisne banaya")

    private fun detectLanguageHint(text: String): LanguageHint =
        if (
            text.contains("tumhe") ||
            text.contains("kisne") ||
            text.contains("banaya") ||
            text.contains("mujhe")
        ) {
            LanguageHint.HINGLISH
        } else {
            LanguageHint.ENGLISH
        }
}
