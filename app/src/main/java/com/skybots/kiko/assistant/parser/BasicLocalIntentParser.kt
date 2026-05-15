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
        if (!containsAny(text, flashlightTokens)) return null

        val type = if (containsAny(text, offTokens)) {
            IntentType.FLASHLIGHT_OFF
        } else {
            IntentType.FLASHLIGHT_ON
        }

        return AssistantIntent(
            type = type,
            rawText = rawText,
            languageHint = languageHint,
        )
    }

    private fun parseVolumeIntent(
        rawText: String,
        text: String,
        languageHint: LanguageHint,
    ): AssistantIntent? {
        if (!containsAny(text, volumeTokens)) return null

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
        if (!containsAny(text, brightnessTokens)) return null

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
        if (!containsAny(text, alarmTokens)) return null

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
        if (!containsAny(text, reminderTokens)) return null

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

    private fun extractPercent(text: String): Int? =
        numberRegex.find(text)
            ?.value
            ?.toIntOrNull()
            ?.coerceIn(0, 100)

    private fun extractAdjustmentDirection(text: String): AdjustmentDirection? =
        when {
            containsAny(text, increaseTokens) -> AdjustmentDirection.INCREASE
            containsAny(text, decreaseTokens) -> AdjustmentDirection.DECREASE
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
            text.contains("lagao") ||
            text.contains("baje") ||
            text.contains("yaad") ||
            text.contains("badhao") ||
            text.contains("kam")
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

        val flashlightTokens = setOf(
            "flashlight",
            "torch",
            "टॉर्च",
        )

        val offTokens = setOf(
            "off",
            "band",
            "बंद",
        )

        val volumeTokens = setOf(
            "volume",
            "awaz",
            "आवाज",
            "आवाज़",
        )

        val brightnessTokens = setOf(
            "brightness",
            "ब्राइटनेस",
        )

        val alarmTokens = setOf(
            "alarm",
            "अलार्म",
        )

        val reminderTokens = setOf(
            "remind",
            "reminder",
            "yaad",
            "याद",
        )

        val increaseTokens = setOf(
            "badhao",
            "increase",
            "up",
            "बढ़ाओ",
            "बढ़ाओ",
        )

        val decreaseTokens = setOf(
            "kam",
            "decrease",
            "down",
            "lower",
            "कम",
        )

        val numberRegex = Regex("\\b\\d{1,3}\\b")
    }
}
