package com.nafaq.expenses.data.parser

import com.nafaq.expenses.data.LexiconEntry
import com.nafaq.expenses.util.StringDistance

data class CategoryMatch(
    val categoryId: String,
    val keyword: String,
    val score: Float
)

class LexiconMatcher(private val entries: List<LexiconEntry>) {

    fun match(phrase: String): CategoryMatch? {
        val folded = ArabicNormalizer.fold(phrase)
        val tokens = folded.split(' ').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        var best: CategoryMatch? = null
        for (entry in entries) {
            val keyword = entry.folded
            if (keyword.isEmpty()) continue
            if (containsSequence(tokens, keyword.split(' '))) {
                val score = if (keyword.contains(' ')) 1f else 0.9f
                if (best == null || keyword.length > best.keyword.length) {
                    best = CategoryMatch(entry.categoryId, entry.keyword, score)
                }
            }
        }
        if (best != null) return best

        return fuzzyMatch(tokens)
    }

    private fun containsSequence(tokens: List<String>, needle: List<String>): Boolean {
        if (needle.isEmpty() || needle.size > tokens.size) return false
        for (start in 0..(tokens.size - needle.size)) {
            var matches = true
            for (offset in needle.indices) {
                if (tokens[start + offset] != needle[offset]) {
                    matches = false
                    break
                }
            }
            if (matches) return true
        }
        return false
    }

    private fun fuzzyMatch(tokens: List<String>): CategoryMatch? {
        var best: CategoryMatch? = null
        for (token in tokens) {
            if (token.length < 4) continue
            for (entry in entries) {
                val keyword = entry.folded
                if (keyword.isEmpty() || keyword.contains(' ') || keyword.length < 4) continue
                val distance = StringDistance.levenshtein(token, keyword)
                val limit = if (keyword.length >= 7) 2 else 1
                if (distance <= limit) {
                    val score = 1f - (distance.toFloat() / (keyword.length + 1f))
                    if (best == null || score > best.score) {
                        best = CategoryMatch(entry.categoryId, entry.keyword, score)
                    }
                } else if (token.startsWith(keyword) || keyword.startsWith(token)) {
                    val score = 0.55f
                    if (best == null || score > best.score) {
                        best = CategoryMatch(entry.categoryId, entry.keyword, score)
                    }
                }
            }
        }
        return best
    }
}
