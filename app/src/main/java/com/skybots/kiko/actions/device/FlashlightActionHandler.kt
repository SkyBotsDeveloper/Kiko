package com.skybots.kiko.actions.device

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.language.LocalizedResponses
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.IntentType

class FlashlightActionHandler(
    private val flashlightController: FlashlightController,
) {
    fun handle(intent: AssistantIntent): AssistantActionResult {
        val enabled = intent.type == IntentType.FLASHLIGHT_ON
        return when (flashlightController.setFlashlight(enabled)) {
            FlashlightControlResult.Success -> AssistantActionResult(
                response = if (enabled) {
                    LocalizedResponses.flashlightOn(intent.languageHint)
                } else {
                    LocalizedResponses.flashlightOff(intent.languageHint)
                },
            )
            FlashlightControlResult.Unavailable -> AssistantActionResult(
                response = LocalizedResponses.flashlightUnavailable(intent.languageHint),
            )
            is FlashlightControlResult.Error -> AssistantActionResult(
                response = LocalizedResponses.flashlightError(intent.languageHint),
            )
        }
    }
}
