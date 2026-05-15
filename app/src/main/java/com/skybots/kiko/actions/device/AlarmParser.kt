package com.skybots.kiko.actions.device

import com.skybots.kiko.utils.TextNormalizer

class AlarmParser {
    fun parse(text: String): AlarmTime? {
        val normalized = TextNormalizer.normalize(text)
        val match = timeRegex.find(normalized) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues.getOrNull(2)
            ?.takeIf { it.isNotBlank() }
            ?.toIntOrNull()
            ?: 0
        val meridiem = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }

        if (hour !in 0..23 || minute !in 0..59) return null

        val hour24 = when {
            meridiem == "pm" && hour < 12 -> hour + 12
            meridiem == "am" && hour == 12 -> 0
            meridiem == "am" || meridiem == "pm" -> hour
            hasEveningHint(normalized) && hour in 1..11 -> hour + 12
            else -> hour
        }

        return AlarmTime(
            hour24 = hour24.coerceIn(0, 23),
            minute = minute,
        )
    }

    private fun hasEveningHint(text: String): Boolean =
        text.contains("raat") ||
            text.contains("shaam") ||
            text.contains("sham") ||
            text.contains("evening") ||
            text.contains("night") ||
            text.contains("रात") ||
            text.contains("शाम")

    private companion object {
        val timeRegex = Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b")
    }
}
