package com.skybots.kiko.assistant.language

enum class ReplyStyle {
    SHORT,
    FRIENDLY,
    PROFESSIONAL,
}

fun replyStyleFrom(value: String?): ReplyStyle =
    ReplyStyle.entries.firstOrNull { it.name == value } ?: ReplyStyle.FRIENDLY
