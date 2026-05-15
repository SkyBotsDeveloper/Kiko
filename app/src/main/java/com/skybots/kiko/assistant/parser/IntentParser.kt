package com.skybots.kiko.assistant.parser

interface IntentParser {
    fun parse(text: String): AssistantIntent
}
