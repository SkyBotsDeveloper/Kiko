package com.skybots.kiko.utils

import android.util.Log
import com.skybots.kiko.assistant.parser.IntentType
import com.skybots.kiko.permissions.KikoPermission

object DiagnosticsLogger {
    private const val TAG = "KikoDiagnostics"
    private const val MAX_DETAIL_LENGTH = 80
    private val phoneLikeDigits = Regex("\\b\\d{4,}\\b")
    private val whitespace = Regex("\\s+")

    fun voiceStart() {
        log("voice_start")
    }

    fun voiceResult(transcript: String) {
        log("voice_result", redactForLog(transcript))
    }

    fun parserIntent(intentType: IntentType) {
        log("parser_intent", intentType.name)
    }

    fun actionRoute(intentType: IntentType) {
        log("action_route", intentType.name)
    }

    fun permissionMissing(permission: KikoPermission) {
        log("permission_missing", permission.name)
    }

    fun actionOutcome(
        action: String,
        success: Boolean,
    ) {
        log("action_outcome", "$action success=$success")
    }

    fun reminderStored(scheduleStatus: String) {
        log("reminder_stored", scheduleStatus)
    }

    fun ttsSpeakStart() {
        log("tts_speak_start")
    }

    fun ttsFailure(reason: String) {
        log("tts_failure", redactForLog(reason))
    }

    fun wakeEvent(
        event: String,
        detail: String = "",
    ) {
        log(event, redactForLog(detail))
    }

    fun redactForLog(value: String): String {
        val compact = value
            .replace(phoneLikeDigits, "[redacted-number]")
            .replace(whitespace, " ")
            .trim()

        return if (compact.length <= MAX_DETAIL_LENGTH) {
            compact
        } else {
            compact.take(MAX_DETAIL_LENGTH) + "..."
        }
    }

    private fun log(
        event: String,
        detail: String = "",
    ) {
        runCatching {
            if (detail.isBlank()) {
                Log.d(TAG, event)
            } else {
                Log.d(TAG, "$event: $detail")
            }
        }
    }
}
