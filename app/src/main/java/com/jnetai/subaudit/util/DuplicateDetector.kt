package com.jnetai.subaudit.util

import com.jnetai.subaudit.data.Subscription

object DuplicateDetector {
    data class DuplicateGroup(val original: Subscription, val duplicates: List<Subscription>)

    fun findDuplicates(subscriptions: List<Subscription>): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()
        val processed = mutableSetOf<String>()

        for (sub in subscriptions) {
            if (sub.id in processed) continue
            val similar = subscriptions.filter { other ->
                other.id != sub.id &&
                other.id !in processed &&
                isSimilar(sub.name, other.name)
            }
            if (similar.isNotEmpty()) {
                groups.add(DuplicateGroup(sub, similar))
                processed.add(sub.id)
                similar.forEach { processed.add(it.id) }
            }
        }
        return groups
    }

    private fun isSimilar(name1: String, name2: String): Boolean {
        val n1 = name1.lowercase().trim()
        val n2 = name2.lowercase().trim()
        if (n1 == n2) return true
        if (n1.contains(n2) || n2.contains(n1)) return true
        // Levenshtein distance check
        val dist = levenshtein(n1, n2)
        val maxLen = maxOf(n1.length, n2.length)
        return maxLen > 0 && dist.toDouble() / maxLen < 0.3
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}