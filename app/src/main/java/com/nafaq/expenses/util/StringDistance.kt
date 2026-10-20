package com.nafaq.expenses.util

object StringDistance {

    fun levenshtein(first: String, second: String): Int {
        if (first == second) return 0
        if (first.isEmpty()) return second.length
        if (second.isEmpty()) return first.length

        var previous = IntArray(second.length + 1) { it }
        var current = IntArray(second.length + 1)

        for (i in first.indices) {
            current[0] = i + 1
            for (j in second.indices) {
                val substitution = previous[j] + if (first[i] == second[j]) 0 else 1
                val insertion = current[j] + 1
                val deletion = previous[j + 1] + 1
                current[j + 1] = minOf(substitution, insertion, deletion)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[second.length]
    }
}
