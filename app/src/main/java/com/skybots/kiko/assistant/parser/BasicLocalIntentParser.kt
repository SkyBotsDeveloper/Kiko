package com.skybots.kiko.assistant.parser

import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.utils.TextNormalizer

class BasicLocalIntentParser : IntentParser {
    override fun parse(text: String): AssistantIntent {
        val normalized = TextNormalizer.normalize(text)
        val languageHint = detectLanguageHint(normalized)

        if (normalized.isBlank()) {
            return AssistantIntent(
                type = IntentType.UNKNOWN,
                rawText = text,
                languageHint = languageHint,
            )
        }

        if (isCreatorQuestion(normalized)) {
            return AssistantIntent(
                type = IntentType.CREATOR_IDENTITY,
                rawText = text,
                languageHint = languageHint,
            )
        }

        val appQuery = extractAppQuery(normalized)
        if (appQuery != null) {
            return AssistantIntent(
                type = IntentType.OPEN_APP,
                rawText = text,
                target = appQuery,
                appQuery = appQuery,
                languageHint = languageHint,
            )
        }

        val contactQuery = extractContactQuery(normalized)
        if (contactQuery != null) {
            return AssistantIntent(
                type = IntentType.CALL_CONTACT,
                rawText = text,
                target = contactQuery,
                contactQuery = contactQuery,
                languageHint = languageHint,
            )
        }

        val type = when {
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

    private fun extractAppQuery(text: String): String? {
        val tokens = TextNormalizer.tokens(text)
        if (tokens.none { it in appTriggerTokens }) return null

        val query = tokens
            .filterNot { it in appCommandTokens }
            .joinToString(" ")
            .trim()

        return query.ifBlank { null }
    }

    private fun extractContactQuery(text: String): String? {
        val tokens = TextNormalizer.tokens(text)
        if (tokens.none { it in callTriggerTokens }) return null

        val query = tokens
            .filterNot { it in callCommandTokens }
            .joinToString(" ")
            .trim()

        return query.ifBlank { null }
    }

    private fun detectLanguageHint(text: String): LanguageHint =
        if (TextNormalizer.containsDevanagari(text)) {
            LanguageHint.HINDI
        } else if (
            text.contains("tumhe") ||
            text.contains("kisne") ||
            text.contains("banaya") ||
            text.contains("mujhe") ||
            text.contains("kholo") ||
            text.contains("khol") ||
            text.contains("karo") ||
            text.contains("lagao")
        ) {
            LanguageHint.HINGLISH
        } else {
            LanguageHint.ENGLISH
        }

    private companion object {
        val appTriggerTokens = setOf(
            "open",
            "khol",
            "kholo",
            "खोलो",
            "खोल",
        )

        val appCommandTokens = setOf(
            "open",
            "khol",
            "kholo",
            "karo",
            "kar",
            "do",
            "app",
            "application",
            "खोलो",
            "खोल",
        )

        val callTriggerTokens = setOf(
            "call",
            "phone",
            "dial",
            "lagao",
            "lagaao",
            "lagana",
            "कॉल",
            "फोन",
        )

        val callCommandTokens = setOf(
            "call",
            "phone",
            "dial",
            "ko",
            "karo",
            "kar",
            "do",
            "lagao",
            "lagaao",
            "lagana",
            "कॉल",
            "फोन",
            "करो",
        )
    }
}
