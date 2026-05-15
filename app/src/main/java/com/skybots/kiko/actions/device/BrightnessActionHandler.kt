package com.skybots.kiko.actions.device

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.language.LocalizedResponses
import com.skybots.kiko.assistant.parser.AdjustmentDirection
import com.skybots.kiko.assistant.parser.AssistantIntent

class BrightnessActionHandler(
    private val brightnessController: BrightnessController,
) {
    fun handle(intent: AssistantIntent): AssistantActionResult {
        val result = when {
            intent.numericValue != null -> brightnessController.setBrightnessPercent(intent.numericValue)
            intent.adjustmentDirection == AdjustmentDirection.INCREASE -> brightnessController.increaseBrightness()
            intent.adjustmentDirection == AdjustmentDirection.DECREASE -> brightnessController.decreaseBrightness()
            else -> brightnessController.increaseBrightness()
        }

        val actionResponse = when {
            intent.numericValue != null -> LocalizedResponses.brightnessPercent(
                intent.numericValue,
                intent.languageHint,
            )
            intent.adjustmentDirection == AdjustmentDirection.DECREASE -> LocalizedResponses.brightnessDecrease(
                intent.languageHint,
            )
            else -> LocalizedResponses.brightnessIncrease(intent.languageHint)
        }

        return when (result) {
            BrightnessControlResult.SystemBrightness -> AssistantActionResult(response = actionResponse)
            BrightnessControlResult.AppBrightnessOnly -> AssistantActionResult(
                response = "$actionResponse ${LocalizedResponses.appBrightnessOnly(intent.languageHint)}",
            )
            is BrightnessControlResult.Error -> AssistantActionResult(
                response = LocalizedResponses.appBrightnessOnly(intent.languageHint),
            )
        }
    }
}
