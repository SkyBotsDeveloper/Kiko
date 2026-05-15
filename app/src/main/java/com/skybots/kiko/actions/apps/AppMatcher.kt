package com.skybots.kiko.actions.apps

import com.skybots.kiko.utils.TextNormalizer
import kotlin.math.max

class AppMatcher {
    fun match(
        query: String,
        apps: List<InstalledApp>,
    ): AppMatchResult {
        val normalizedQuery = normalizeAlias(TextNormalizer.normalize(query))
        if (normalizedQuery.isBlank()) return AppMatchResult.None

        val scored = apps
            .map { app -> app to score(normalizedQuery, app) }
            .filter { (_, score) -> score >= MIN_SCORE }
            .sortedWith(
                compareByDescending<Pair<InstalledApp, Int>> { it.second }
                    .thenBy { it.first.label.length },
            )

        val best = scored.firstOrNull() ?: return AppMatchResult.None
        val second = scored.drop(1).firstOrNull()

        return when {
            best.second == 100 -> AppMatchResult.Single(best.first)
            best.second >= CONFIDENT_SCORE &&
                (second == null || best.second - second.second >= CONFIDENT_GAP) -> {
                AppMatchResult.Single(best.first)
            }
            scored.size == 1 && best.second >= SOLO_SCORE -> AppMatchResult.Single(best.first)
            scored.size > 1 -> AppMatchResult.Multiple(scored.take(MAX_CANDIDATES).map { it.first })
            else -> AppMatchResult.None
        }
    }

    private fun score(
        query: String,
        app: InstalledApp,
    ): Int {
        val label = TextNormalizer.normalize(app.label)
        val packageName = TextNormalizer.normalize(app.packageName)
        return when {
            query == label -> 100
            label.startsWith(query) -> 90
            label.split(" ").any { it == query } -> 88
            label.contains(query) -> 80
            packageName.contains(query) -> 68
            else -> similarityScore(query, label)
        }
    }

    private fun normalizeAlias(query: String): String =
        aliases[query] ?: query

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
        val aliases = mapOf(
            "insta" to "instagram",
            "ig" to "instagram",
            "wa" to "whatsapp",
            "whatsapp" to "whatsapp",
            "yt" to "youtube",
            "youtube" to "youtube",
            "chrome" to "chrome",
            "telegram" to "telegram",
        )

        const val MIN_SCORE = 64
        const val SOLO_SCORE = 72
        const val CONFIDENT_SCORE = 86
        const val CONFIDENT_GAP = 12
        const val MAX_CANDIDATES = 5
    }
}
