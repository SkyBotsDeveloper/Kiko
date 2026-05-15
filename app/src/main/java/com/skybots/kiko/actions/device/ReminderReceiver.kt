package com.skybots.kiko.actions.device

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.skybots.kiko.R

class ReminderReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (!canPostNotifications(context)) return

        val message = intent.getStringExtra(EXTRA_REMINDER_MESSAGE).orEmpty()
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, System.currentTimeMillis())
        createChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_kiko_orb)
            .setContentTitle("Kiko reminder")
            .setContentText(message.ifBlank { "Reminder" })
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(reminderId.toInt(), notification)
        }
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Kiko reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_REMINDER_ID = "kiko.extra.REMINDER_ID"
        const val EXTRA_REMINDER_MESSAGE = "kiko.extra.REMINDER_MESSAGE"
        private const val CHANNEL_ID = "kiko_reminders"
    }
}
