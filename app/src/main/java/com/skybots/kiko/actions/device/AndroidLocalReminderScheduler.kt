package com.skybots.kiko.actions.device

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.skybots.kiko.permissions.PermissionChecker

class AndroidLocalReminderScheduler(
    context: Context,
    private val permissionChecker: PermissionChecker,
) : LocalReminderScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(reminder: Reminder): ReminderScheduleResult {
        if (!permissionChecker.hasPostNotificationsPermission()) {
            return ReminderScheduleResult.NotificationPermissionMissing
        }

        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_REMINDER_MESSAGE, reminder.message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return runCatching {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerAtMillis,
                pendingIntent,
            )
        }.fold(
            onSuccess = { ReminderScheduleResult.Scheduled },
            onFailure = { error -> ReminderScheduleResult.Failed(error.message) },
        )
    }
}
