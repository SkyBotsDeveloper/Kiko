package com.skybots.kiko.actions.device

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.language.LocalizedResponses
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.permissions.KikoPermission
import com.skybots.kiko.utils.DiagnosticsLogger
import java.util.Calendar

class ReminderActionHandler(
    private val reminderParser: ReminderParser,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: LocalReminderScheduler,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) {
    fun handle(intent: AssistantIntent): AssistantActionResult {
        val request = reminderParser.parse(intent.rawText)
        val alarmTime = request.alarmTime
            ?: return AssistantActionResult(
                response = LocalizedResponses.reminderNeedsTime(intent.languageHint),
            )
        val message = request.message
            ?: return AssistantActionResult(
                response = LocalizedResponses.reminderNeedsMessage(intent.languageHint),
            )

        val reminder = Reminder(
            id = clockMillis(),
            triggerAtMillis = computeTriggerAtMillis(alarmTime, request.dayOffset),
            message = message,
        )
        reminderRepository.save(reminder)

        val baseResponse = LocalizedResponses.reminderSaved(
            displayTime = alarmTime.displayText,
            message = message,
            dayOffset = request.dayOffset,
            languageHint = intent.languageHint,
        )

        return when (reminderScheduler.schedule(reminder)) {
            ReminderScheduleResult.Scheduled -> {
                DiagnosticsLogger.reminderStored("scheduled")
                AssistantActionResult(response = baseResponse)
            }
            ReminderScheduleResult.NotificationPermissionMissing -> {
                DiagnosticsLogger.reminderStored("stored_notification_permission_missing")
                AssistantActionResult(
                    response = baseResponse + LocalizedResponses.notificationPermissionNeeded(intent.languageHint),
                    requestedPermission = KikoPermission.POST_NOTIFICATIONS,
                )
            }
            is ReminderScheduleResult.Failed -> {
                DiagnosticsLogger.reminderStored("stored_schedule_failed")
                AssistantActionResult(response = baseResponse)
            }
        }
    }

    private fun computeTriggerAtMillis(
        alarmTime: AlarmTime,
        dayOffset: Int,
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = clockMillis()
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, alarmTime.hour24)
            set(Calendar.MINUTE, alarmTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (dayOffset == 0 && calendar.timeInMillis <= clockMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }
}
