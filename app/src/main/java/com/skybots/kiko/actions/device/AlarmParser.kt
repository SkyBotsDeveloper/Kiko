package com.skybots.kiko.actions.device

import com.skybots.kiko.utils.TextNormalizer
import java.util.Locale

class AlarmParser {
    fun parse(text: String): AlarmTime? {
        val normalized = TextNormalizer.normalize(text)
        val match = TIME_REGEX.find(text.lowercase(Locale.ROOT)) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues.getOrNull(2)
            ?.takeIf { it.isNotBlank() }
            ?.toIntOrNull()
            ?: 0
        val meridiem = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }

        if (minute !in 0..59) return null
        if (hour !in 0..23) return null
        if ((meridiem == "am" || meridiem == "pm") && hour !in 1..12) return null

        return AlarmTime(
            hour24 = toHour24(hour, meridiem, normalized),
            minute = minute,
            dayOffset = dayOffset(normalized),
        )
    }

    fun dayOffset(text: String): Int {
        val normalized = TextNormalizer.normalize(text)
        return when {
            containsAny(normalized, TOMORROW_TOKENS) -> 1
            containsAny(normalized, TODAY_TOKENS) -> 0
            else -> 0
        }
    }

    private fun toHour24(
        hour: Int,
        meridiem: String?,
        text: String,
    ): Int =
        when {
            meridiem == "pm" && hour < 12 -> hour + 12
            meridiem == "am" && hour == 12 -> 0
            meridiem == "am" || meridiem == "pm" -> hour
            hasAfternoonHint(text) && hour in 1..11 -> hour + 12
            hasEveningHint(text) && hour in 1..11 -> hour + 12
            else -> hour
        }.coerceIn(0, 23)

    private fun hasAfternoonHint(text: String): Boolean =
        containsAny(text, AFTERNOON_TOKENS)

    private fun hasEveningHint(text: String): Boolean =
        containsAny(text, EVENING_TOKENS)

    private fun containsAny(
        text: String,
        tokens: Set<String>,
    ): Boolean {
        val words = TextNormalizer.tokens(text).toSet()
        return tokens.any { token -> words.contains(token) || text.contains(token) }
    }

    private companion object {
        val TIME_REGEX = Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b")

        val TODAY_TOKENS = setOf("today", "aaj", "\u0906\u091c")
        val TOMORROW_TOKENS = setOf("tomorrow", "kal", "\u0915\u0932")
        val AFTERNOON_TOKENS = setOf("dopahar", "afternoon", "\u0926\u094b\u092a\u0939\u0930")
        val EVENING_TOKENS = setOf(
            "shaam",
            "sham",
            "raat",
            "evening",
            "night",
            "\u0936\u093e\u092e",
            "\u0930\u093e\u0924",
        )
    }
}
