package com.nafaq.expenses.data.parser

data class PriceMatch(
    val amount: Double?,
    val currency: String?,
    val consumed: Set<Int>
)

object PriceExtractor {

    private val currencyWords: Map<String, String> = linkedMapOf(
        "دولارات" to "دولار",
        "دولار" to "دولار",
        "دينارات" to "دينار",
        "دينار" to "دينار",
        "دن" to "دينار",
        "د.ا" to "دينار",
        "ريالات" to "ريال",
        "ريال" to "ريال",
        "شوال" to "ريال",
        "شيكلات" to "شيكل",
        "شيكل" to "شيكل",
        "دراهم" to "درهم",
        "درهم" to "درهم",
        "ليرات" to "ليرة",
        "ليرة" to "ليرة",
        "ل.س" to "ليرة",
        "جنيهات" to "جنيه",
        "جنيه" to "جنيه",
        "قروش" to "قرش",
        "قرش" to "قرش",
        "هللات" to "هلله",
        "هلله" to "هلله",
        "فلس" to "فلس",
        "فلوس" to "فلوس",
        "يورو" to "يورو",
        "usd" to "دولار",
        "eur" to "يورو",
        "jod" to "دينار",
        "sar" to "ريال",
        "ils" to "شيكل",
        "aed" to "درهم",
        "egp" to "جنيه",
        "try" to "ليرة"
    )

    private val noiseWords = setOf("و", "ب", "بسعر", "ثمن", "مبلغ", "سعر", "قيمته", "فقط")

    private val priceMarkers = setOf("بسعر", "ثمن", "مبلغ", "سعر", "قيمته", "ب")

    private val unitWords = setOf(
        "كيلو", "كيلوغرام", "كيلوجرام", "كجم", "جم", "غرام", "غرام",
        "لتر", "لترين", "حبة", "حبه", "علبة", "علبه", "كيس", "كيسه",
        "قطعة", "قطعه", "رغيف", "قنينة", "قنونه", "عبوة", "عبوه",
        "دزينة", "كرتون", "عدد", "باكيت", "رطل", "شكوه", "شكل"
    )

    private val digitsRegex = Regex("^\\d+(\\.\\d+)?$")

    fun extract(foldedText: String): PriceMatch {
        val tokens = foldedText.split(' ').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return PriceMatch(null, null, emptySet())

        val currencyIndex = tokens.indexOfFirst { currencyWords.containsKey(it) }
        if (currencyIndex >= 0) {
            val currency = currencyWords[tokens[currencyIndex]]
            val left = collect(tokens, currencyIndex, -1)
            if (left.value != null && left.value > 0.0) {
                return PriceMatch(left.value, currency, left.indices + currencyIndex)
            }
            val right = collect(tokens, currencyIndex, 1)
            if (right.value != null && right.value > 0.0) {
                return PriceMatch(right.value, currency, right.indices + currencyIndex)
            }
            return PriceMatch(null, currency, setOf(currencyIndex))
        }

        return detectWithoutCurrency(tokens)
    }

    private data class Collected(val value: Double?, val indices: Set<Int>)

    private fun collect(tokens: List<String>, index: Int, step: Int): Collected {
        var position = index + step
        var total = 0.0
        var found = false
        val indices = mutableSetOf<Int>()
        while (position in tokens.indices) {
            val token = tokens[position]
            if (token in noiseWords) {
                position += step
                continue
            }
            val value = numberValue(token) ?: break
            if (step > 0 && isUnit(tokens.getOrNull(position + 1))) break
            total += value
            found = true
            indices += position
            position += step
        }
        return Collected(if (found) total else null, indices)
    }

    private fun detectWithoutCurrency(tokens: List<String>): PriceMatch {
        val candidates = mutableListOf<Triple<Int, Double, Boolean>>()
        for (index in tokens.indices) {
            val token = tokens[index]
            val value = numberValue(token) ?: continue
            if (isUnit(tokens.getOrNull(index + 1))) continue
            candidates += Triple(index, value, isPriceMarker(tokens.getOrNull(index - 1)))
        }
        if (candidates.isEmpty()) return PriceMatch(null, null, emptySet())
        val marked = candidates.filter { it.third }
        val chosen = when {
            marked.isNotEmpty() -> marked.last()
            candidates.size == 1 -> candidates.first()
            else -> null
        } ?: return PriceMatch(null, null, emptySet())
        return PriceMatch(chosen.second, null, setOf(chosen.first))
    }

    private fun isPriceMarker(token: String?): Boolean {
        val cleaned = token?.trim() ?: return false
        return cleaned in priceMarkers || cleaned == "و"
    }

    private fun isUnit(token: String?): Boolean {
        val cleaned = token?.trim() ?: return false
        return cleaned in unitWords
    }

    private fun numberValue(token: String): Double? {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.matches(digitsRegex)) return trimmed.toDouble()
        NumberWords.value(trimmed)?.let { return it }
        if (trimmed.length > 1) {
            val first = trimmed[0]
            if (first == 'و' || first == 'ب') {
                val rest = trimmed.substring(1)
                if (rest.matches(digitsRegex)) return rest.toDouble()
                NumberWords.value(rest)?.let { return it }
            }
        }
        return null
    }
}
