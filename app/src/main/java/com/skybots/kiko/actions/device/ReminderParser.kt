package com.skybots.kiko.actions.device

import com.skybots.kiko.utils.TextNormalizer

class ReminderParser(
    private val alarmParser: AlarmParser = AlarmParser(),
) {
    fun parse(text: String): ReminderRequest {
        val normalized = TextNormalizer.normalize(text)
        return ReminderRequest(
            alarmTime = alarmParser.parse(text),
            message = extractMessage(text, normalized),
            dayOffset = if (normalized.contains("kal") || normalized.contains("कल")) 1 else 0,
        )
    }

    private fun extractMessage(
        rawText: String,
        normalized: String,
    ): String? {
        val englishMessage = englishToRegex.find(rawText)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
        if (!englishMessage.isNullOrBlank()) return englishMessage.trimEnd('.', ',', ' ')

        if (normalized.contains("yaad")) {
            return extractBeforeYaad(normalized)
        }

        if (normalized.contains("याद")) {
            return extractBeforeYaad(normalized)
        }

        return null
    }

    private fun extractBeforeYaad(normalized: String): String? {
        val beforeYaad = normalized
            .substringBefore("yaad")
            .substringBefore("याद")
        val message = TextNormalizer.tokens(beforeYaad)
            .filterNot { token -> token in fillerTokens || token.toIntOrNull() != null }
            .joinToString(" ")
            .trim()

        return message.ifBlank { null }
    }

    private companion object {
        val englishToRegex = Regex("\\bto\\s+(.+)$", RegexOption.IGNORE_CASE)

        val fillerTokens = setOf(
            "remind",
            "reminder",
            "me",
            "mujhe",
            "at",
            "baje",
            "pm",
            "am",
            "kal",
            "subah",
            "raat",
            "shaam",
            "sham",
            "laga",
            "lagao",
            "dilana",
            "do",
            "मुझे",
            "बजे",
            "कल",
        )
    }
}
