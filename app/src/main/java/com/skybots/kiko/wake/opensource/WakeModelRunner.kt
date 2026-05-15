package com.skybots.kiko.wake.opensource

interface WakeModelRunner {
    fun load(): WakeEngineHealth

    fun score(frame: WakeAudioFrame): Float

    fun release()

    fun isReady(): Boolean
}
