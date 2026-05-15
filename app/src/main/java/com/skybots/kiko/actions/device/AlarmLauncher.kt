package com.skybots.kiko.actions.device

interface AlarmLauncher {
    fun setAlarm(alarmTime: AlarmTime): Boolean
}
