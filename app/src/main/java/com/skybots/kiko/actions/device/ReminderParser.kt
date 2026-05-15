package com.skybots.kiko.actions.device

import com.skybots.kiko.utils.TextNormalizer

class ReminderParser(
    private val alarmParser: AlarmParser = AlarmParser(),
) {
    fun parse(text: String): ReminderRequest {
        val normalized = TextNormalizer.normalize(text)
        val alarmTime = alarmParser.parse(text)
        return ReminderRequest(
            alarmTime = alarmTime,
            message = extractMessage(text, normalized),
            dayOffset = alarmTime?.dayOffset ?: alarmParser.dayOffset(text),
        )
    }

    private fun extractMessage(
        rawText: String,
        normalized: String,
    ): String? {
        val englishMessage = ENGLISH_TO_REGEX.find(rawText)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
        if (!englishMessage.isNullOrBlank()) return englishMessage.trimEnd('.', ',', ' ')

        if (normalized.contains("yaad") || normalized.contains(HINDI_YAAD)) {
            return extractBeforeYaad(normalized)
        }

        return null
    }

    private fun extractBeforeYaad(normalized: String): String? {
        val beforeYaad = normalized
            .substringBefore("yaad")
            .substringBefore(HINDI_YAAD)
        val message = TextNormalizer.tokens(beforeYaad)
            .filterNot { token -> token in FILLER_TOKENS || token.toIntOrNull() != null }
            .joinToString(" ")
            .trim()

        return message.ifBlank { null }
    }

    private companion object {
        val ENGLISH_TO_REGEX = Regex("\\bto\\s+(.+)$", RegexOption.IGNORE_CASE)
        const val HINDI_YAAD = "\u092f\u093e\u0926"

        val FILLER_TOKENS = setOf(
            "remind",
            "reminder",
            "me",
            "mujhe",
            "at",
            "baje",
            "pm",
            "am",
            "kal",
            "aaj",
            "today",
            "tomorrow",
            "subah",
            "dopahar",
            "raat",
            "shaam",
            "sham",
            "laga",
            "lagao",
            "dilana",
            "do",
            "\u092e\u0941\u091d\u0947",
            "\u092c\u091c\u0947",
            "\u0915\u0932",
            "\u0906\u091c",
            "\u0938\u0941\u092c\u0939",
            "\u0926\u094b\u092a\u0939\u0930",
            "\u0930\u093e\u0924",
            "\u0936\u093e\u092e",
            "\u0926\u093f\u0932\u093e\u0928\u093e",
        )
    }
}
