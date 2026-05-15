package com.skybots.kiko.assistant.parser

import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.utils.TextNormalizer

class BasicLocalIntentParser : IntentParser {
    override fun parse(text: String): AssistantIntent {
        val normalized = TextNormalizer.normalize(text)
        val languageHint = detectLanguageHint(text)

        if (normalized.isBlank()) {
            return intent(IntentType.UNKNOWN, text, languageHint)
        }

        if (isCreatorQuestion(normalized)) {
            return intent(IntentType.CREATOR_IDENTITY, text, languageHint)
        }

        parseFlashlightIntent(text, normalized, languageHint)?.let { return it }
        parseVolumeIntent(text, normalized, languageHint)?.let { return it }
        parseBrightnessIntent(text, normalized, languageHint)?.let { return it }
        parseAlarmIntent(text, normalized, languageHint)?.let { return it }
        parseReminderIntent(text, normalized, languageHint)?.let { return it }

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

        return intent(
            type = if (isInternetRequiredQuery(normalized)) {
                IntentType.INTERNET_REQUIRED_QUERY
            } else {
                IntentType.UNKNOWN
            },
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

        return intent(
            type = if (containsAny(text, FLASHLIGHT_OFF_TOKENS)) {
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
            numericValue = extractPercent(text) ?: volumeKeywordPercent(text),
            adjustmentDirection = extractAdjustmentDirection(text),
            languageHint = languageHint,
        )
    }

    private fun parseBrightnessIntent(
        rawText: String,
        text: String,
        languageHint: LanguageHint,
    ): AssistantIntent? {
        val brightnessRequest = containsAny(text, BRIGHTNESS_TOKENS) ||
            (containsAny(text, SCREEN_TOKENS) && containsAny(text, DECREASE_TOKENS))
        if (!brightnessRequest) return null

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

        return intent(IntentType.SET_ALARM, rawText, languageHint)
    }

    private fun parseReminderIntent(
        rawText: String,
        text: String,
        languageHint: LanguageHint,
    ): AssistantIntent? {
        if (!containsAny(text, REMINDER_TOKENS)) return null

        return intent(IntentType.SET_REMINDER, rawText, languageHint)
    }

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
        if (tokens.none { it in CONTACT_TRIGGER_TOKENS }) return null

        val query = tokens
            .filterNot { it in CONTACT_COMMAND_TOKENS }
            .joinToString(" ")
            .trim()

        return query.ifBlank { null }
    }

    private fun isCreatorQuestion(text: String): Boolean =
        CREATOR_PHRASES.any { phrase -> text.contains(phrase) }

    private fun isInternetRequiredQuery(text: String): Boolean =
        INTERNET_QUERY_PHRASES.any { phrase -> text.contains(phrase) }

    private fun extractPercent(text: String): Int? =
        NUMBER_REGEX.find(text)
            ?.value
            ?.toIntOrNull()
            ?.coerceIn(0, 100)

    private fun volumeKeywordPercent(text: String): Int? =
        when {
            containsAny(text, FULL_VOLUME_TOKENS) -> 100
            containsAny(text, MUTE_VOLUME_TOKENS) -> 0
            else -> null
        }

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
        return tokens.any { token -> words.contains(token) || text.contains(token) }
    }

    private fun detectLanguageHint(text: String): LanguageHint {
        val normalized = TextNormalizer.normalize(text)
        return when {
            TextNormalizer.containsDevanagari(text) -> LanguageHint.HINDI
            HINGLISH_LANGUAGE_TOKENS.any { normalized.contains(it) } -> LanguageHint.HINGLISH
            else -> LanguageHint.ENGLISH
        }
    }

    private fun intent(
        type: IntentType,
        rawText: String,
        languageHint: LanguageHint,
    ): AssistantIntent =
        AssistantIntent(
            type = type,
            rawText = rawText,
            languageHint = languageHint,
        )

    private companion object {
        val APP_TRIGGER_TOKENS = setOf(
            "open",
            "khol",
            "kholo",
            "chalao",
            "\u0916\u094b\u0932",
            "\u0916\u094b\u0932\u094b",
            "\u091a\u0932\u093e\u0913",
        )
        val APP_COMMAND_TOKENS = APP_TRIGGER_TOKENS + setOf(
            "karo",
            "kar",
            "do",
            "app",
            "application",
            "\u0915\u0930\u094b",
        )

        val CONTACT_TRIGGER_TOKENS = setOf(
            "call",
            "phone",
            "dial",
            "\u0915\u0949\u0932",
            "\u092b\u094b\u0928",
        )
        val CONTACT_COMMAND_TOKENS = CONTACT_TRIGGER_TOKENS + setOf(
            "ko",
            "karo",
            "kar",
            "do",
            "lagao",
            "lagaao",
            "lagana",
            "\u0915\u094b",
            "\u0915\u0930\u094b",
            "\u0932\u0917\u093e\u0913",
        )

        val FLASHLIGHT_TOKENS = setOf("flashlight", "torch", "\u091f\u0949\u0930\u094d\u091a")
        val FLASHLIGHT_OFF_TOKENS = setOf("off", "band", "bujha", "bujhao", "\u092c\u0902\u0926", "\u092c\u0941\u091d\u093e")

        val VOLUME_TOKENS = setOf("volume", "awaz", "awaaz", "\u0906\u0935\u093e\u091c", "\u0906\u0935\u093e\u091c\u093c")
        val FULL_VOLUME_TOKENS = setOf("full", "max", "maximum")
        val MUTE_VOLUME_TOKENS = setOf("mute", "silent", "zero")

        val BRIGHTNESS_TOKENS = setOf("brightness", "\u092c\u094d\u0930\u093e\u0907\u091f\u0928\u0947\u0938")
        val SCREEN_TOKENS = setOf("screen", "display")
        val ALARM_TOKENS = setOf("alarm", "\u0905\u0932\u093e\u0930\u094d\u092e")
        val REMINDER_TOKENS = setOf("remind", "reminder", "yaad", "\u092f\u093e\u0926")

        val INCREASE_TOKENS = setOf("badhao", "increase", "up", "\u092c\u0922\u093c\u093e\u0913", "\u092c\u095d\u093e\u0913")
        val DECREASE_TOKENS = setOf("kam", "decrease", "down", "lower", "dim", "\u0915\u092e")

        val CREATOR_PHRASES = setOf(
            "who created you",
            "who made you",
            "who is your creator",
            "tumhe kisne banaya",
            "kiko ko kisne banaya",
            "kisne banaya",
            "\u0924\u0941\u092e\u094d\u0939\u0947\u0902 \u0915\u093f\u0938\u0928\u0947 \u092c\u0928\u093e\u092f\u093e",
            "\u0924\u0941\u092e\u094d\u0939\u093e\u0930\u093e creator \u0915\u094c\u0928 \u0939\u0948",
            "\u0915\u093f\u0938\u0928\u0947 \u092c\u0928\u093e\u092f\u093e",
        )

        val INTERNET_QUERY_PHRASES = setOf(
            "weather",
            "latest news",
            "news batao",
            "search online",
            "online search",
            "internet se search",
            "internet",
            "google",
            "explain",
            "mujhse baat karo",
            "baat karo",
            "chat karo",
            "search",
        )

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
            "chalao",
            "subah",
            "shaam",
            "raat",
        )
        val NUMBER_REGEX = Regex("\\b\\d{1,3}\\b")
    }
}
