package com.skybots.kiko.wake

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.skybots.kiko.MainActivity
import com.skybots.kiko.R
import com.skybots.kiko.wake.opensource.WakeEngineHealthStatus

class WakeWordNotificationHelper(
    private val context: Context,
) {
    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(NotificationManager::class.java)

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Kiko Wake Word",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Foreground service for optional Hey Kiko wake-word listening."
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun listeningNotification(
        phrase: String = WakeWordConfig.DEFAULT_PHRASE,
        calibrationStatus: WakeCalibrationStatus = WakeCalibrationStatus.COLLECTING_BASELINE,
        modelStatus: WakeEngineHealthStatus = WakeEngineHealthStatus.READY,
    ): Notification {
        val content = WakeNotificationStateMapper.contentFor(
            phrase = phrase,
            engineState = WakeWordEngineState.Listening,
            calibrationStatus = calibrationStatus,
            modelStatus = modelStatus,
        )
        return serviceNotification(content)
    }

    fun pausedLockedNotification(): Notification =
        serviceNotification(
            WakeNotificationStateMapper.contentFor(
                phrase = WakeWordConfig.DEFAULT_PHRASE,
                engineState = WakeWordEngineState.PausedLocked,
                calibrationStatus = WakeCalibrationStatus.COLLECTING_BASELINE,
                modelStatus = WakeEngineHealthStatus.READY,
            ),
        )

    fun serviceNotification(content: WakeNotificationContent): Notification =
        baseBuilder()
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_kiko_orb,
                "Stop wake word",
                servicePendingIntent(WakeWordService.ACTION_STOP, REQUEST_STOP),
            )
            .addAction(
                R.drawable.ic_kiko_orb,
                "Open Kiko",
                openKikoPendingIntent(),
            )
            .build()

    fun wakeDetectedNotification(): Notification =
        baseBuilder()
            .setContentTitle("Hey Kiko detected")
            .setContentText("Tap to speak to Kiko")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_kiko_orb,
                "Stop wake word",
                servicePendingIntent(WakeWordService.ACTION_STOP, REQUEST_STOP),
            )
            .addAction(
                R.drawable.ic_kiko_orb,
                "Tap to speak to Kiko",
                openKikoPendingIntent(),
            )
            .build()

    @SuppressLint("MissingPermission")
    fun showWakeDetectedNotification() {
        runCatching {
            NotificationManagerCompat.from(appContext).notify(
                NOTIFICATION_ID,
                wakeDetectedNotification(),
            )
        }.onFailure { error ->
            WakeWordDiagnostics.error(error.message ?: "Could not update wake notification.")
        }
    }

    @SuppressLint("MissingPermission")
    fun showListeningNotification(
        phrase: String,
        calibrationStatus: WakeCalibrationStatus,
        modelStatus: WakeEngineHealthStatus,
    ) {
        runCatching {
            NotificationManagerCompat.from(appContext).notify(
                NOTIFICATION_ID,
                listeningNotification(phrase, calibrationStatus, modelStatus),
            )
        }.onFailure { error ->
            WakeWordDiagnostics.error(error.message ?: "Could not update wake notification.")
        }
    }

    @SuppressLint("MissingPermission")
    fun showPausedLockedNotification() {
        runCatching {
            NotificationManagerCompat.from(appContext).notify(
                NOTIFICATION_ID,
                pausedLockedNotification(),
            )
        }.onFailure { error ->
            WakeWordDiagnostics.error(error.message ?: "Could not update wake notification.")
        }
    }

    private fun baseBuilder(): NotificationCompat.Builder =
        NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_kiko_orb)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openKikoPendingIntent())

    private fun servicePendingIntent(
        action: String,
        requestCode: Int,
    ): PendingIntent =
        PendingIntent.getService(
            appContext,
            requestCode,
            Intent(appContext, WakeWordService::class.java).setAction(action),
            pendingIntentFlags(),
        )

    private fun openKikoPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            appContext,
            REQUEST_OPEN,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            pendingIntentFlags(),
        )

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    companion object {
        const val CHANNEL_ID = "kiko_wake_word"
        const val NOTIFICATION_ID = 2101
        private const val REQUEST_STOP = 2102
        private const val REQUEST_OPEN = 2103
    }
}
