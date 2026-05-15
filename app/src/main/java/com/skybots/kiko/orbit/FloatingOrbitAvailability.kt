package com.skybots.kiko.orbit

enum class FloatingOrbitAvailability {
    Active,
    PermissionNeeded,
    InAppFallback,
}

object FloatingOrbitAvailabilityMapper {
    fun map(
        enabled: Boolean,
        overlayPermissionGranted: Boolean,
    ): FloatingOrbitAvailability =
        when {
            enabled && overlayPermissionGranted -> FloatingOrbitAvailability.Active
            enabled -> FloatingOrbitAvailability.PermissionNeeded
            else -> FloatingOrbitAvailability.InAppFallback
        }
}
