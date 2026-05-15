package com.skybots.kiko.wake

import java.util.concurrent.CopyOnWriteArraySet

object WakeWordRuntime {
    private val listeners = CopyOnWriteArraySet<(WakeWordEvent) -> Unit>()

    @Volatile
    private var state: WakeWordEngineState = WakeWordEngineState.Disabled

    fun currentState(): WakeWordEngineState = state

    fun updateState(newState: WakeWordEngineState) {
        state = newState
    }

    fun publish(event: WakeWordEvent) {
        state = when (event) {
            WakeWordEvent.Started -> WakeWordEngineState.Listening
            WakeWordEvent.Stopped -> WakeWordEngineState.Stopped
            WakeWordEvent.WakeDetected -> WakeWordEngineState.WakeDetected
            is WakeWordEvent.Error -> WakeWordEngineState.Error
        }
        listeners.forEach { listener -> listener(event) }
    }

    fun subscribe(listener: (WakeWordEvent) -> Unit): () -> Unit {
        listeners += listener
        return { listeners -= listener }
    }

    fun resetForTests() {
        listeners.clear()
        state = WakeWordEngineState.Disabled
    }
}
