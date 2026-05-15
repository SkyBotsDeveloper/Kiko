package com.skybots.kiko.actions.device

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.language.LocalizedResponses
import com.skybots.kiko.assistant.parser.AssistantIntent

class AlarmActionHandler(
    private val alarmParser: AlarmParser,
    private val alarmLauncher: AlarmLauncher,
) {
    fun handle(intent: AssistantIntent): AssistantActionResult {
        val alarmTime = alarmParser.parse(intent.rawText)
            ?: return AssistantActionResult(
                response = LocalizedResponses.alarmNeedsTime(intent.languageHint),
            )

        return if (alarmLauncher.setAlarm(alarmTime)) {
            AssistantActionResult(
                response = LocalizedResponses.alarmSet(
                    displayTime = alarmTime.displayText,
                    languageHint = intent.languageHint,
                ),
            )
        } else {
            AssistantActionResult(
                response = LocalizedResponses.alarmLaunchFailed(intent.languageHint),
            )
        }
    }
}
