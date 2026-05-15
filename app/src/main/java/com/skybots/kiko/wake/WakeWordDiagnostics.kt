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

    fun error(message: String) {
        DiagnosticsLogger.wakeEvent("wake_error", message)
    }
}
