package com.skybots.kiko.wake

import com.skybots.kiko.memory.InMemoryMemoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordServiceControllerTest {
    @Test
    fun wakeWordIsDisabledByDefault() {
        val repository = InMemoryMemoryRepository()

        assertFalse(repository.getUserPreferences().wakeWordEnabled)
        assertEquals("Hey Kiko", repository.getUserPreferences().wakeWordPhrase)
        assertEquals("fake", repository.getUserPreferences().wakeWordEngine)
    }

    @Test
    fun permissionMissingDoesNotStartService() {
        WakeWordRuntime.resetForTests()
        val repository = InMemoryMemoryRepository()
        val serviceStarter = FakeWakeWordServiceStarter()
        val controller = controller(
            repository = repository,
            hasAudioPermission = false,
            serviceStarter = serviceStarter,
        )

        val result = controller.enableWakeWord()

        assertFalse(result.success)
        assertEquals(WakeWordEngineState.PermissionMissing, result.state)
        assertFalse(repository.getUserPreferences().wakeWordEnabled)
        assertFalse(serviceStarter.started)
    }

    @Test
    fun enableWakeWordPersistsPreferenceAndStartsService() {
        WakeWordRuntime.resetForTests()
        val repository = InMemoryMemoryRepository()
        val serviceStarter = FakeWakeWordServiceStarter()
        val controller = controller(
            repository = repository,
            hasAudioPermission = true,
            serviceStarter = serviceStarter,
        )

        val result = controller.enableWakeWord()

        assertTrue(result.success)
        assertTrue(repository.getUserPreferences().wakeWordEnabled)
        assertTrue(serviceStarter.started)
    }

    @Test
    fun statusMapsEnabledPreferenceAndRuntimeState() {
        WakeWordRuntime.resetForTests()
        val repository = InMemoryMemoryRepository()
        val serviceStarter = FakeWakeWordServiceStarter()
        val controller = controller(
            repository = repository,
            hasAudioPermission = true,
            serviceStarter = serviceStarter,
        )

        controller.enableWakeWord()
        WakeWordRuntime.updateState(WakeWordEngineState.Listening)

        assertEquals(WakeWordEngineState.Listening, controller.getStatus())
    }

    @Test
    fun simulateWakeDetectionUsesServiceStarterOnlyWhenEnabled() {
        WakeWordRuntime.resetForTests()
        val repository = InMemoryMemoryRepository()
        val serviceStarter = FakeWakeWordServiceStarter()
        val controller = controller(
            repository = repository,
            hasAudioPermission = true,
            serviceStarter = serviceStarter,
        )

        assertFalse(controller.simulateWakeDetection().success)

        controller.enableWakeWord()
        val result = controller.simulateWakeDetection()

        assertTrue(result.success)
        assertTrue(serviceStarter.simulated)
    }

    private fun controller(
        repository: InMemoryMemoryRepository,
        hasAudioPermission: Boolean,
        serviceStarter: WakeWordServiceStarter,
    ): WakeWordServiceController =
        WakeWordServiceController(
            hasRecordAudioPermission = { hasAudioPermission },
            getPreferences = repository::getUserPreferences,
            updatePreferences = repository::updateUserPreferences,
            serviceStarter = serviceStarter,
        )

    private class FakeWakeWordServiceStarter : WakeWordServiceStarter {
        var started = false
        var stopped = false
        var simulated = false

        override fun startWakeService() {
            started = true
        }

        override fun stopWakeService() {
            stopped = true
        }

        override fun simulateWakeDetection() {
            simulated = true
        }
    }
}
