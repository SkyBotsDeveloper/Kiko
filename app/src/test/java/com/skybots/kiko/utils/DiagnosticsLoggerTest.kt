package com.skybots.kiko.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsLoggerTest {
    @Test
    fun redactionRemovesPhoneLikeNumbers() {
        val redacted = DiagnosticsLogger.redactForLog("call mummy on 9876543210")

        assertFalse(redacted.contains("9876543210"))
        assertTrue(redacted.contains("[redacted-number]"))
    }

    @Test
    fun redactionTruncatesLongText() {
        val redacted = DiagnosticsLogger.redactForLog("a".repeat(160))

        assertTrue(redacted.length < 100)
        assertTrue(redacted.endsWith("..."))
    }

    @Test
    fun wakeDiagnosticsUseSharedRedactionRules() {
        val redacted = DiagnosticsLogger.redactForLog("wake failed near 9876543210")

        assertFalse(redacted.contains("9876543210"))
        assertTrue(redacted.contains("[redacted-number]"))
    }
}
