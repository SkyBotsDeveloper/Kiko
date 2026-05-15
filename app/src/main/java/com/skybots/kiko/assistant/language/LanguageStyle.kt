package com.skybots.kiko.assistant.language

enum class LanguageStyle {
    AUTO,
    ENGLISH,
    HINGLISH,
    HINDI,
    UNKNOWN,
}

fun LanguageStyle.toLanguageHint(): LanguageHint =
    when (this) {
        LanguageStyle.ENGLISH -> LanguageHint.ENGLISH
        LanguageStyle.HINGLISH -> LanguageHint.HINGLISH
        LanguageStyle.HINDI -> LanguageHint.HINDI
        LanguageStyle.AUTO,
        LanguageStyle.UNKNOWN -> LanguageHint.SYSTEM_DEFAULT
    }

fun languageStyleFrom(value: String?): LanguageStyle =
    LanguageStyle.entries.firstOrNull { it.name == value } ?: LanguageStyle.AUTO
