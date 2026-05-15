package com.skybots.kiko.utils

import java.util.Locale

object TextNormalizer {
    private val punctuationRegex = Regex("[^\\p{L}\\p{N}\\s]")
    private val whitespaceRegex = Regex("\\s+")

    fun normalize(value: String): String =
        value
            .lowercase(Locale.ROOT)
            .replace(punctuationRegex, " ")
            .replace(whitespaceRegex, " ")
            .trim()

    fun tokens(value: String): List<String> =
        normalize(value)
            .split(" ")
            .filter { it.isNotBlank() }

    fun containsDevanagari(value: String): Boolean =
        value.any { character -> character in '\u0900'..'\u097F' }
}
