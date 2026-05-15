package com.skybots.kiko.assistant.parser

import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.utils.TextNormalizer

class BasicLocalIntentParser : IntentParser {
    override fun parse(text: String): AssistantIntent {
        val normalized = TextNormalizer.normalize(text)
        val languageHint = detectLanguageHint(text)

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

        extractAppQuery(normalized)?.let { appQuery ->
            return AssistantIntent(
                type = IntentType.OPEN_APP,
                rawText = text,
                target = appQuery,
                appQuery = appQuery,
                languageHint = languageHint,
            )
        }

        extractContactQuery(normalized)?.let { contactQuery ->
            return AssistantIntent(
                type = IntentType.CALL_CONTACT,
                rawText = text,
                target = contactQuery,
                contactQuery = contactQuery,
                languageHint = languageHint,
            )
        }

        parseFlashlightIntent(text, normalized, languageHint)?.let { return it }
        parseVolumeIntent(text, normalized, languageHint)?.let { return it }
        parseBrightnessIntent(text, normalized, languageHint)?.let { return it }
        parseAlarmIntent(text, normalized, languageHint)?.let { return it }
        parseReminderIntent(text, normalized, languageHint)?.let { return it }

        val type = when {
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

    private fun parseFlashlightIntent(
        rawText: String,
        text: String,
        languageHint: LanguageHint,
    ): AssistantIntent? {
        if (!containsAny(text, FLASHLIGHT_TOKENS)) return null

        return AssistantIntent(
            type = if (containsAny(text, OFF_TOKENS)) {
                IntentType.FLASHLIGHT_OFF
            } else {
                IntentType.FLASHLIGHT_ON
            },
            rawText = rawText,
            languageHint = languageHint,
        )
    }

    private fun parseVolumeIntent(
        rawText: String,
        text: String,
        languageHint: LanguageHint,
    ): AssistantIntent? {
        if (!containsAny(text, VOLUME_TOKENS)) return null

        return AssistantIntent(
            type = IntentType.SET_VOLUME,
            rawText = rawText,
            numericValue = extractPercent(text),
            adjustmentDirection = extractAdjustmentDirection(text),
            languageHint = languageHint,
        )
    }

    private fun parseBrightnessIntent(
        rawText: String,
        text: String,
        languageHint: LanguageHint,
    ): AssistantIntent? {
        if (!containsAny(text, BRIGHTNESS_TOKENS)) return null

        return AssistantIntent(
            type = IntentType.SET_BRIGHTNESS,
            rawText = rawText,
            numericValue = extractPercent(text),
            adjustmentDirection = extractAdjustmentDirection(text),
            languageHint = languageHint,
        )
    }

    private fun parseAlarmIntent(
        rawText: String,
        text: String,
        languageHint: LanguageHint,
    ): AssistantIntent? {
        if (!containsAny(text, ALARM_TOKENS)) return null

        return AssistantIntent(
            type = IntentType.SET_ALARM,
            rawText = rawText,
            languageHint = languageHint,
        )
    }

    private fun parseReminderIntent(
        rawText: String,
        text: String,
        languageHint: LanguageHint,
    ): AssistantIntent? {
        if (!containsAny(text, REMINDER_TOKENS)) return null

        return AssistantIntent(
            type = IntentType.SET_REMINDER,
            rawText = rawText,
            languageHint = languageHint,
        )
    }

    private fun isCreatorQuestion(text: String): Boolean =
        text.contains("who created you") ||
            text.contains("who made you") ||
            text.contains("tumhe kisne banaya") ||
            text.contains("kisne banaya") ||
            text.contains(HINDI_CREATOR_QUESTION)

    private fun extractAppQuery(text: String): String? {
        val tokens = TextNormalizer.tokens(text)
        if (tokens.none { it in APP_TRIGGER_TOKENS }) return null

        val query = tokens
            .filterNot { it in APP_COMMAND_TOKENS }
            .joinToString(" ")
            .trim()

        return query.ifBlank { null }
    }

    private fun extractContactQuery(text: String): String? {
        val tokens = TextNormalizer.tokens(text)
        if (tokens.none { it in CALL_TRIGGER_TOKENS }) return null

        val query = tokens
            .filterNot { it in CALL_COMMAND_TOKENS }
            .joinToString(" ")
            .trim()

        return query.ifBlank { null }
    }

    private fun extractPercent(text: String): Int? =
        NUMBER_REGEX.find(text)
            ?.value
            ?.toIntOrNull()
            ?.coerceIn(0, 100)

    private fun extractAdjustmentDirection(text: String): AdjustmentDirection? =
        when {
            containsAny(text, INCREASE_TOKENS) -> AdjustmentDirection.INCREASE
            containsAny(text, DECREASE_TOKENS) -> AdjustmentDirection.DECREASE
            else -> null
        }

    private fun containsAny(
        text: String,
        tokens: Set<String>,
    ): Boolean {
        val words = TextNormalizer.tokens(text).toSet()
        return tokens.any { token ->
            words.contains(token) || text.contains(token)
        }
    }

    private fun detectLanguageHint(text: String): LanguageHint {
        val normalized = TextNormalizer.normalize(text)
        return if (TextNormalizer.containsDevanagari(text)) {
            LanguageHint.HINDI
        } else if (
            HINGLISH_LANGUAGE_TOKENS.any { normalized.contains(it) }
        ) {
            LanguageHint.HINGLISH
        } else {
            LanguageHint.ENGLISH
        }
    }

    private companion object {
        val APP_TRIGGER_TOKENS = setOf("open", "khol", "kholo", "खोलो", "खोल")
        val APP_COMMAND_TOKENS = setOf(
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
            "करो",
        )

        val CALL_TRIGGER_TOKENS = setOf("call", "phone", "dial", "lagao", "lagaao", "lagana", "कॉल", "फोन")
        val CALL_COMMAND_TOKENS = setOf(
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

        val FLASHLIGHT_TOKENS = setOf("flashlight", "torch", "टॉर्च")
        val OFF_TOKENS = setOf("off", "band", "बंद")
        val VOLUME_TOKENS = setOf("volume", "awaz", "आवाज", "आवाज़")
        val BRIGHTNESS_TOKENS = setOf("brightness", "ब्राइटनेस")
        val ALARM_TOKENS = setOf("alarm", "अलार्म")
        val REMINDER_TOKENS = setOf("remind", "reminder", "yaad", "याद")
        val INCREASE_TOKENS = setOf("badhao", "increase", "up", "बढ़ाओ", "बढ़ाओ")
        val DECREASE_TOKENS = setOf("kam", "decrease", "down", "lower", "कम")
        val HINGLISH_LANGUAGE_TOKENS = setOf(
            "tumhe",
            "kisne",
            "banaya",
            "mujhe",
            "kholo",
            "khol",
            "karo",
            "lagao",
            "baje",
            "yaad",
            "badhao",
            "kam",
        )
        val NUMBER_REGEX = Regex("\\b\\d{1,3}\\b")
        const val HINDI_CREATOR_QUESTION = "\u0915\u093f\u0938\u0928\u0947 \u092c\u0928\u093e\u092f\u093e"
    }
}
