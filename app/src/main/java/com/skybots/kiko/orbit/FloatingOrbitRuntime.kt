package com.skybots.kiko.orbit

import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

sealed class FloatingOrbitCommand {
    data object StartManualMic : FloatingOrbitCommand()
    data object OpenSettings : FloatingOrbitCommand()
}

object FloatingOrbitRuntime {
    private val listeners = CopyOnWriteArraySet<(FloatingOrbitCommand) -> Unit>()
    private val pendingManualMic = AtomicBoolean(false)
    private val pendingOpenSettings = AtomicBoolean(false)

    fun requestManualMic() {
        if (listeners.isEmpty()) {
            pendingManualMic.set(true)
        }
        publish(FloatingOrbitCommand.StartManualMic)
    }

    fun requestOpenSettings() {
        if (listeners.isEmpty()) {
            pendingOpenSettings.set(true)
        }
        publish(FloatingOrbitCommand.OpenSettings)
    }

    fun consumePendingManualMic(): Boolean = pendingManualMic.getAndSet(false)

    fun consumePendingOpenSettings(): Boolean = pendingOpenSettings.getAndSet(false)

    fun subscribe(listener: (FloatingOrbitCommand) -> Unit): () -> Unit {
        listeners += listener
        return { listeners -= listener }
    }

    fun resetForTests() {
        listeners.clear()
        pendingManualMic.set(false)
        pendingOpenSettings.set(false)
    }

    private fun publish(command: FloatingOrbitCommand) {
        listeners.forEach { listener -> listener(command) }
    }
}
