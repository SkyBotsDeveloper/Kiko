package com.skybots.kiko.actions.device

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.IntentType

class RealDeviceActionHandler(
    private val flashlightActionHandler: FlashlightActionHandler,
    private val volumeActionHandler: VolumeActionHandler,
    private val brightnessActionHandler: BrightnessActionHandler,
    private val alarmActionHandler: AlarmActionHandler,
    private val reminderActionHandler: ReminderActionHandler,
) : DeviceActionHandler {
    override fun handle(intent: AssistantIntent): AssistantActionResult =
        when (intent.type) {
            IntentType.FLASHLIGHT_ON,
            IntentType.FLASHLIGHT_OFF -> flashlightActionHandler.handle(intent)
            IntentType.SET_VOLUME -> volumeActionHandler.handle(intent)
            IntentType.SET_BRIGHTNESS -> brightnessActionHandler.handle(intent)
            IntentType.SET_ALARM -> alarmActionHandler.handle(intent)
            IntentType.SET_REMINDER -> reminderActionHandler.handle(intent)
            else -> AssistantActionResult(response = "Device controls will be added in the next phase.")
        }
}
