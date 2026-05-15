package com.skybots.kiko.orbit

import android.content.Context

data class FloatingOrbitControlResult(
    val success: Boolean,
    val permissionGranted: Boolean,
    val message: String,
)

class FloatingOrbitController(
    private val context: Context,
    private val permissionHelper: FloatingOrbitPermissionHelper = FloatingOrbitPermissionHelper(context),
    private val settingsStore: FloatingOrbitSettingsStore = FloatingOrbitSettingsStore(context),
) {
    fun settings(): FloatingOrbitSettings = settingsStore.read()

    fun hasOverlayPermission(): Boolean = permissionHelper.canDrawOverlays()

    fun setEnabled(enabled: Boolean): FloatingOrbitControlResult {
        settingsStore.update(FloatingOrbitSettings(enabled = enabled))
        return if (enabled) {
            start()
        } else {
            stop()
        }
    }

    fun startIfEnabled(): FloatingOrbitControlResult {
        val permissionGranted = hasOverlayPermission()
        return when (FloatingOrbitAvailabilityMapper.map(settingsStore.read().enabled, permissionGranted)) {
            FloatingOrbitAvailability.Active -> start()
            FloatingOrbitAvailability.PermissionNeeded -> FloatingOrbitControlResult(
                success = false,
                permissionGranted = false,
                message = "Floating orbit needs Android overlay permission. In-app orbit fallback is active.",
            )
            FloatingOrbitAvailability.InAppFallback -> FloatingOrbitControlResult(
                success = false,
                permissionGranted = permissionGranted,
                message = "Floating orbit is off.",
            )
        }
    }

    fun start(): FloatingOrbitControlResult {
        if (!hasOverlayPermission()) {
            return FloatingOrbitControlResult(
                success = false,
                permissionGranted = false,
                message = "Android overlay permission is needed for the floating orbit. In-app orbit remains available.",
            )
        }
        context.applicationContext.startService(FloatingOrbitService.startIntent(context.applicationContext))
        return FloatingOrbitControlResult(
            success = true,
            permissionGranted = true,
            message = "Floating orbit is active.",
        )
    }

    fun stop(): FloatingOrbitControlResult {
        context.applicationContext.startService(
            FloatingOrbitService.stopIntent(context.applicationContext),
        )
        return FloatingOrbitControlResult(
            success = true,
            permissionGranted = hasOverlayPermission(),
            message = "Floating orbit stopped.",
        )
    }

    fun openOverlaySettings() {
        context.startActivity(
            permissionHelper.overlaySettingsIntent().addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
