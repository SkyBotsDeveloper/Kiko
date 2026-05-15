package com.skybots.kiko.wake.opensource

interface WakeAudioSource {
    fun start(
        onFrame: (WakeAudioFrame) -> Unit,
        onError: (WakeEngineHealth) -> Unit,
    ): WakeEngineHealth

    fun stop()

    fun release()

    fun isRunning(): Boolean
}
