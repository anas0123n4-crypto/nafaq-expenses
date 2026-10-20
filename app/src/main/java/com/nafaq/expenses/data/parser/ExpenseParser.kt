package com.nafaq.expenses.data.parser

data class ParsedDraft(
    val rawText: String,
    val itemName: String,
    val categoryId: String?,
    val amount: Double?,
    val currency: String?,
    val matchedKeyword: String?,
    val confidence: Float,
    val source: String = "voice"
)

class ExpenseParser(private val matcher: LexiconMatcher) {

    private val verbs = setOf(
        "اشتريت", "شريت", "اشترى", "اشترت", "اشترينا", "دفعت", "دفع",
        "خلصت", "انفقت", "نفدت", "طلبت", "اخذت", "جبت", "وردت", "حضرت",
        "اكلت", "اكل", "تناولت", "شربت", "تسوقت", "بعت", "حجزت", "سجلت",
        "اشتري", "buy", "bought", "paid"
    )

    private val timeFillers = setOf(
        "اليوم", "الحين", "البارح", "صباحا", "صباح", "مساء", "ليلا", "بليل", "النهيرة"
    )

    private val prepositions = setOf("من", "عند", "في", "على", "لدي", "عنده", "عندها")

    private val trailingFillers = setOf(
        "و", "من", "عند", "في", "على", "ب", "ل", "بسعر", "ثمن", "مبلغ", "اليوم", "الحين"
    )

    fun parse(text: String): ParsedDraft {
        val normalized = ArabicNormalizer.normalize(text)
        val folded = ArabicNormalizer.fold(normalized)
        val foldedTokens = folded.split(' ').filter { it.isNotBlank() }
        val normalizedTokens = normalized.split(' ').filter { it.isNotBlank() }

        val price = PriceExtractor.extract(folded)
        val itemTokens = normalizedTokens.filterIndexed { index, _ -> index !in price.consumed }
        val itemName = cleanItem(itemTokens)
        val target = if (itemName.isNotBlank()) itemName else normalized
        val match = matcher.match(target)

        return ParsedDraft(
            rawText = text.trim(),
            itemName = itemName.ifBlank { "مصروف" },
            categoryId = match?.categoryId,
            amount = price.amount,
            currency = price.currency,
            matchedKeyword = match?.keyword,
            confidence = match?.score ?: 0f
        )
    }

    private fun cleanItem(tokens: List<String>): String {
        val list = tokens.toMutableList()

        while (list.isNotEmpty()) {
            val first = list.first()
            if (first in verbs || first in timeFillers) {
                list.removeAt(0)
            } else {
                break
            }
        }

        while (true) {
            val index = list.indexOfFirst { it in prepositions }
            if (index < 0) break
            list.removeAt(index)
            if (index < list.size && list[index] !in prepositions) {
                list.removeAt(index)
            }
        }

        while (list.isNotEmpty()) {
            val last = list.last()
            if (last in trailingFillers) {
                list.removeAt(list.size - 1)
            } else {
                break
            }
        }

        return list.joinToString(" ").trim()
    }
}
