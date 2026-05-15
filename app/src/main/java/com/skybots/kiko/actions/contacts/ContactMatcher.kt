package com.skybots.kiko.actions.contacts

import com.skybots.kiko.utils.TextNormalizer
import kotlin.math.max

class ContactMatcher {
    fun match(
        query: String,
        contacts: List<ContactModel>,
    ): ContactMatchResult {
        val normalizedQuery = TextNormalizer.normalize(query)
        if (normalizedQuery.isBlank()) return ContactMatchResult.None

        val scored = contacts
            .map { contact -> contact to score(normalizedQuery, TextNormalizer.normalize(contact.displayName)) }
            .filter { (_, score) -> score >= MIN_SCORE }
            .sortedWith(
                compareByDescending<Pair<ContactModel, Int>> { it.second }
                    .thenBy { it.first.displayName.lowercase() },
            )

        return when {
            scored.isEmpty() -> ContactMatchResult.None
            scored.size == 1 -> ContactMatchResult.Single(scored.first().first)
            else -> ContactMatchResult.Multiple(scored.take(MAX_CANDIDATES).map { it.first })
        }
    }

    private fun score(
        query: String,
        candidate: String,
    ): Int =
        when {
            query == candidate -> 100
            candidate.startsWith(query) -> 92
            candidate.split(" ").any { it == query } -> 88
            candidate.contains(query) -> 82
            query.contains(candidate) -> 68
            else -> similarityScore(query, candidate)
        }

    private fun similarityScore(
        query: String,
        candidate: String,
    ): Int {
        val maxLength = max(query.length, candidate.length)
        if (maxLength == 0) return 0

        val distance = levenshteinDistance(query, candidate)
        val similarity = 1.0 - (distance.toDouble() / maxLength.toDouble())
        return (similarity * 100).toInt()
    }

    private fun levenshteinDistance(
        left: String,
        right: String,
    ): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        val previous = IntArray(right.length + 1) { it }
        val current = IntArray(right.length + 1)

        for (i in 1..left.length) {
            current[0] = i
            for (j in 1..right.length) {
                val substitutionCost = if (left[i - 1] == right[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + substitutionCost,
                )
            }
            current.copyInto(previous)
        }

        return previous[right.length]
    }

    private companion object {
        const val MIN_SCORE = 68
        const val MAX_CANDIDATES = 5
    }
}
