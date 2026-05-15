package com.skybots.kiko.actions.device

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

class AndroidAlarmLauncher(context: Context) : AlarmLauncher {
    private val appContext = context.applicationContext

    override fun setAlarm(alarmTime: AlarmTime): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, alarmTime.hour24)
            putExtra(AlarmClock.EXTRA_MINUTES, alarmTime.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Kiko alarm")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return runCatching {
            appContext.startActivity(intent)
        }.isSuccess
    }
}
