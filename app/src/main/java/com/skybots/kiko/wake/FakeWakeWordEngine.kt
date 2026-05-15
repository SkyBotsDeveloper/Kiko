package com.skybots.kiko.wake

class FakeWakeWordEngine : WakeWordEngine {
    private var listener: ((WakeWordEvent) -> Unit)? = null
    private var running = false
    private var released = false

    override fun start() {
        if (released) {
            listener?.invoke(WakeWordEvent.Error("Wake engine has already been released."))
            return
        }
        running = true
        listener?.invoke(WakeWordEvent.Started)
    }

    override fun stop() {
        if (!running) return
        running = false
        listener?.invoke(WakeWordEvent.Stopped)
    }

    override fun release() {
        if (running) {
            stop()
        }
        listener = null
        released = true
    }

    override fun isRunning(): Boolean = running

    override fun setEventListener(listener: ((WakeWordEvent) -> Unit)?) {
        this.listener = listener
    }

    fun simulateWakeDetection() {
        if (running) {
            listener?.invoke(WakeWordEvent.WakeDetected)
        } else {
            listener?.invoke(WakeWordEvent.Error("Wake engine is not listening."))
        }
    }
}
