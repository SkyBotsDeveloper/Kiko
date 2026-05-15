package com.skybots.kiko.wake

import com.skybots.kiko.utils.DiagnosticsLogger

object WakeWordDiagnostics {
    fun serviceStart() {
        DiagnosticsLogger.wakeEvent("wake_service_start")
    }

    fun serviceStop() {
        DiagnosticsLogger.wakeEvent("wake_service_stop")
    }

    fun permissionMissing() {
        DiagnosticsLogger.wakeEvent("wake_permission_missing")
    }

    fun engineStart(engine: String) {
        DiagnosticsLogger.wakeEvent("wake_engine_start", engine)
    }

    fun engineStop() {
        DiagnosticsLogger.wakeEvent("wake_engine_stop")
    }

    fun wakeDetected() {
        DiagnosticsLogger.wakeEvent("wake_detected")
    }

    fun notificationShown() {
        DiagnosticsLogger.wakeEvent("wake_notification_shown")
    }

    fun wakeFlowStarted() {
        DiagnosticsLogger.wakeEvent("wake_flow_started")
    }

    fun openSourceEngineSelected() {
        DiagnosticsLogger.wakeEvent("wake_open_source_engine_selected")
    }

    fun modelStatus(status: String) {
        DiagnosticsLogger.wakeEvent("wake_model_status", status)
    }

    fun modelInputShape(shape: String) {
        DiagnosticsLogger.wakeEvent("wake_model_input_shape", shape)
    }

    fun featureTypeSelected(featureType: String) {
        DiagnosticsLogger.wakeEvent("wake_feature_type_selected", featureType)
    }

    fun modelReady(status: String) {
        DiagnosticsLogger.wakeEvent("wake_model_ready", status)
    }

    fun audioSourceStart() {
        DiagnosticsLogger.wakeEvent("wake_audio_source_start")
    }

    fun audioSourceStop() {
        DiagnosticsLogger.wakeEvent("wake_audio_source_stop")
    }

    fun modelInferenceError(message: String) {
        DiagnosticsLogger.wakeEvent("wake_model_inference_error", message)
    }

    fun thresholdCrossed(score: Float) {
        DiagnosticsLogger.wakeEvent("wake_score_threshold_crossed", "score=${"%.3f".format(score)}")
    }

    fun debouncePrevented() {
        DiagnosticsLogger.wakeEvent("wake_debounce_prevented")
    }

    fun error(message: String) {
        DiagnosticsLogger.wakeEvent("wake_error", message)
    }
}
