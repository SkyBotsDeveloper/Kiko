package com.skybots.kiko.wake.features

class PcmRingBuffer(
    private val capacity: Int,
) {
    private val buffer = ShortArray(capacity)
    private var writeIndex = 0
    private var sampleCount = 0

    val isFilled: Boolean
        get() = sampleCount >= capacity

    fun append(
        samples: ShortArray,
        count: Int,
    ) {
        val safeCount = count.coerceIn(0, samples.size)
        for (index in 0 until safeCount) {
            buffer[writeIndex] = samples[index]
            writeIndex = (writeIndex + 1) % capacity
        }
        sampleCount = (sampleCount + safeCount).coerceAtMost(capacity)
    }

    fun copyWindow(target: ShortArray) {
        require(target.size == capacity) {
            "Target window must match ring buffer capacity."
        }
        val start = if (isFilled) writeIndex else 0
        for (index in 0 until capacity) {
            target[index] = buffer[(start + index) % capacity]
        }
    }

    fun clear() {
        buffer.fill(0)
        writeIndex = 0
        sampleCount = 0
    }
}
