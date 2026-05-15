package com.skybots.kiko.wake.features

import kotlin.math.PI
import kotlin.math.cos

object WindowFunction {
    fun periodicHann(size: Int): FloatArray {
        require(size > 0) { "Window size must be positive." }
        return FloatArray(size) { index ->
            (0.5 - (0.5 * cos((2.0 * PI * index) / size))).toFloat()
        }
    }
}
