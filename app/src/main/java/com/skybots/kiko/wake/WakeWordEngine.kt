package com.skybots.kiko.wake

interface WakeWordEngine {
    fun start()

    fun stop()

    fun release()

    fun isRunning(): Boolean

    fun setEventListener(listener: ((WakeWordEvent) -> Unit)?)
}
