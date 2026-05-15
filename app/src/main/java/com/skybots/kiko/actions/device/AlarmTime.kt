package com.skybots.kiko.actions.device

data class AlarmTime(
    val hour24: Int,
    val minute: Int = 0,
) {
    val displayText: String
        get() {
            val period = if (hour24 < 12) "AM" else "PM"
            val hour12 = when (val hour = hour24 % 12) {
                0 -> 12
                else -> hour
            }
            return "%d:%02d %s".format(hour12, minute, period)
        }
}
