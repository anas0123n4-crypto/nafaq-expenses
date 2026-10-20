package com.nafaq.expenses

import com.nafaq.expenses.data.LexiconEntry
import com.nafaq.expenses.data.parser.ArabicNormalizer
import com.nafaq.expenses.data.parser.ExpenseParser
import com.nafaq.expenses.data.parser.LexiconMatcher
import com.nafaq.expenses.data.parser.PriceExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicEngineTest {

    private val entries = listOf(
        entry("sweets", "سدر كنافه"),
        entry("sweets", "كنافة"),
        entry("sweets", "شيبس"),
        entry("sweets", "بسكويت"),
        entry("food", "رز"),
        entry("food", "زيت"),
        entry("readyfood", "شاورما"),
        entry("cleaning", "منظف أرضيات")
    )

    private val matcher = LexiconMatcher(entries)
    private val parser = ExpenseParser(matcher)

    private fun entry(categoryId: String, keyword: String) =
        LexiconEntry(categoryId, keyword, ArabicNormalizer.fold(keyword))

    @Test
    fun translatesArabicIndicDigits() {
        assertEquals("اشتريت 1250 دينار", ArabicNormalizer.normalize("اشتريت ١٢٥٠ دينار"))
    }

    @Test
    fun foldsArabicLetterVariants() {
        assertEquals("سدر كنافه", ArabicNormalizer.fold("سِدْر كَنَافَة"))
    }

    @Test
    fun extractsPriceFromSpelledNumber() {
        val price = PriceExtractor.extract(ArabicNormalizer.fold("اشتريت سدر كنافه بعشر دنانير"))
        assertEquals(10.0, price.amount!!, 0.001)
        assertEquals("دينار", price.currency)
    }

    @Test
    fun extractsPriceWithDigits() {
        val price = PriceExtractor.extract(ArabicNormalizer.fold("اشتريت رز بـ 25 دينار"))
        assertEquals(25.0, price.amount!!, 0.001)
        assertEquals("دينار", price.currency)
    }

    @Test
    fun extractsDecimalPrice() {
        val price = PriceExtractor.extract(ArabicNormalizer.fold("3 كيلو لحم بـ 15.5 دينار"))
        assertEquals(15.5, price.amount!!, 0.001)
    }

    @Test
    fun extractsPriceWithoutCurrency() {
        val price = PriceExtractor.extract(ArabicNormalizer.fold("اشتriet شيبس بـ 12"))
        assertEquals(12.0, price.amount!!, 0.001)
        assertNull(price.currency)
    }

    @Test
    fun ignoresQuantityNumbers() {
        val price = PriceExtractor.extract(ArabicNormalizer.fold("اشتريت 3 كيلو لحم بـ 20 دينار"))
        assertEquals(20.0, price.amount!!, 0.001)
    }

    @Test
    fun matchesExactLexiconEntry() {
        val match = matcher.match("سدر كنافه")
        assertNotNull(match)
        assertEquals("sweets", match!!.categoryId)
    }

    @Test
    fun matchesFuzzySpelling() {
        val match = matcher.match("سدر كنافة")
        assertNotNull(match)
        assertEquals("sweets", match!!.categoryId)
    }

    @Test
    fun parsesFullSentence() {
        val draft = parser.parse("اشتريت سدر كنافه بعشر دنانير")
        assertEquals("سدر كنافه", draft.itemName)
        assertEquals("sweets", draft.categoryId)
        assertEquals(10.0, draft.amount!!, 0.001)
        assertEquals("دينار", draft.currency)
    }

    @Test
    fun parsesSentenceWithPunctuationAndNoise() {
        val draft = parser.parse("اليوم اشتريت من السوق شاورما دجاج بـ 18 دينار")
        assertEquals("شاورما دجاج", draft.itemName)
        assertEquals("readyfood", draft.categoryId)
        assertEquals(18.0, draft.amount!!, 0.001)
    }

    @Test
    fun keepsUnknownItemsWithoutCategory() {
        val draft = parser.parse("اشتريت شي غريب بـ 5 دينار")
        assertNull(draft.categoryId)
        assertTrue(draft.itemName.isNotBlank())
    }
}
