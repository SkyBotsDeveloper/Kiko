package com.skybots.kiko.orbit

import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingOrbitAvailabilityMapperTest {
    @Test
    fun enabledWithPermissionIsActive() {
        assertEquals(
            FloatingOrbitAvailability.Active,
            FloatingOrbitAvailabilityMapper.map(
                enabled = true,
                overlayPermissionGranted = true,
            ),
        )
    }

    @Test
    fun enabledWithoutPermissionUsesPermissionNeededFallback() {
        assertEquals(
            FloatingOrbitAvailability.PermissionNeeded,
            FloatingOrbitAvailabilityMapper.map(
                enabled = true,
                overlayPermissionGranted = false,
            ),
        )
    }

    @Test
    fun disabledUsesInAppFallback() {
        assertEquals(
            FloatingOrbitAvailability.InAppFallback,
            FloatingOrbitAvailabilityMapper.map(
                enabled = false,
                overlayPermissionGranted = true,
            ),
        )
    }
}
