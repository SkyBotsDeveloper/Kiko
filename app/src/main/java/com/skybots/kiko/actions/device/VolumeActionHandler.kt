package com.skybots.kiko.actions.device

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.language.LocalizedResponses
import com.skybots.kiko.assistant.parser.AdjustmentDirection
import com.skybots.kiko.assistant.parser.AssistantIntent

class VolumeActionHandler(
    private val volumeController: VolumeController,
) {
    fun handle(intent: AssistantIntent): AssistantActionResult {
        val result = when {
            intent.numericValue != null -> volumeController.setMediaVolumePercent(intent.numericValue)
            intent.adjustmentDirection == AdjustmentDirection.INCREASE -> volumeController.increaseMediaVolume()
            intent.adjustmentDirection == AdjustmentDirection.DECREASE -> volumeController.decreaseMediaVolume()
            else -> volumeController.increaseMediaVolume()
        }

        return when (result) {
            VolumeControlResult.Success -> AssistantActionResult(
                response = when {
                    intent.numericValue != null -> LocalizedResponses.volumePercent(
                        intent.numericValue,
                        intent.languageHint,
                    )
                    intent.adjustmentDirection == AdjustmentDirection.DECREASE -> LocalizedResponses.volumeDecrease(
                        intent.languageHint,
                    )
                    else -> LocalizedResponses.volumeIncrease(intent.languageHint)
                },
            )
            VolumeControlResult.FixedVolume -> AssistantActionResult(
                response = LocalizedResponses.volumeFixed(intent.languageHint),
            )
            is VolumeControlResult.Error -> AssistantActionResult(
                response = LocalizedResponses.volumeFixed(intent.languageHint),
            )
        }
    }
}
