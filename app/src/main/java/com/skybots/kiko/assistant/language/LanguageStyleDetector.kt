package com.skybots.kiko.assistant.language

import com.skybots.kiko.utils.TextNormalizer

class LanguageStyleDetector {
    fun detect(text: String): LanguageStyle {
        val normalized = TextNormalizer.normalize(text)
        if (normalized.isBlank()) return LanguageStyle.UNKNOWN

        return when {
            TextNormalizer.containsDevanagari(text) -> LanguageStyle.HINDI
            containsHinglish(normalized) -> LanguageStyle.HINGLISH
            normalized.any { it in 'a'..'z' } -> LanguageStyle.ENGLISH
            else -> LanguageStyle.UNKNOWN
        }
    }

    fun resolve(
        inputText: String,
        preferredLanguageStyle: LanguageStyle,
    ): LanguageHint =
        when (preferredLanguageStyle) {
            LanguageStyle.ENGLISH -> LanguageHint.ENGLISH
            LanguageStyle.HINGLISH -> LanguageHint.HINGLISH
            LanguageStyle.HINDI -> LanguageHint.HINDI
            LanguageStyle.AUTO,
            LanguageStyle.UNKNOWN -> when (detect(inputText)) {
                LanguageStyle.ENGLISH -> LanguageHint.ENGLISH
                LanguageStyle.HINGLISH -> LanguageHint.HINGLISH
                LanguageStyle.HINDI -> LanguageHint.HINDI
                LanguageStyle.AUTO,
                LanguageStyle.UNKNOWN -> LanguageHint.SYSTEM_DEFAULT
            }
        }

    private fun containsHinglish(text: String): Boolean =
        HINGLISH_MARKERS.any { marker -> text.contains(marker) }

    private companion object {
        val HINGLISH_MARKERS = setOf(
            "baje",
            "badhao",
            "band",
            "banaya",
            "chahiye",
            "chalu",
            "haan",
            "karo",
            "karu",
            "kisne",
            "kholo",
            "kam",
            "lagao",
            "mujhe",
            "nahi",
            "padhai",
            "subah",
            "tumhe",
            "yaad",
        )
    }
}
