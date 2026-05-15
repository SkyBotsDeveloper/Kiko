package com.skybots.kiko.wake

import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

object WakeWordRuntime {
    private val listeners = CopyOnWriteArraySet<(WakeWordEvent) -> Unit>()
    private val pendingWakeLaunch = AtomicBoolean(false)

    @Volatile
    private var state: WakeWordEngineState = WakeWordEngineState.Disabled

    @Volatile
    private var scoreSnapshot: WakeScoreSnapshot = WakeScoreSnapshot()

    fun currentState(): WakeWordEngineState = state

    fun currentScoreSnapshot(): WakeScoreSnapshot = scoreSnapshot

    fun updateState(newState: WakeWordEngineState) {
        state = newState
    }

    fun publish(event: WakeWordEvent) {
        state = when (event) {
            WakeWordEvent.Started -> WakeWordEngineState.Listening
            WakeWordEvent.Stopped -> WakeWordEngineState.Stopped
            WakeWordEvent.PausedLocked -> WakeWordEngineState.PausedLocked
            WakeWordEvent.WakeDetected -> WakeWordEngineState.WakeDetected
            is WakeWordEvent.ScoreDebug -> state
            is WakeWordEvent.Error -> WakeWordEngineState.Error
        }
        if (event is WakeWordEvent.ScoreDebug) {
            scoreSnapshot = event.snapshot
        }
        listeners.forEach { listener -> listener(event) }
    }

    fun markPendingWakeLaunch() {
        pendingWakeLaunch.set(true)
    }

    fun consumePendingWakeLaunch(): Boolean = pendingWakeLaunch.getAndSet(false)

    fun publishScore(snapshot: WakeScoreSnapshot) {
        publish(WakeWordEvent.ScoreDebug(snapshot))
    }

    fun resetScore() {
        scoreSnapshot = WakeScoreSnapshot()
    }

    fun subscribe(listener: (WakeWordEvent) -> Unit): () -> Unit {
        listeners += listener
        return { listeners -= listener }
    }

    fun resetForTests() {
        listeners.clear()
        state = WakeWordEngineState.Disabled
        scoreSnapshot = WakeScoreSnapshot()
        pendingWakeLaunch.set(false)
    }
}
