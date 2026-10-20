package com.nafaq.expenses.data

import android.content.Context
import com.nafaq.expenses.data.parser.ArabicNormalizer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LexiconCategoryFile(
    val id: String,
    val keywords: List<String>
)

@Serializable
data class LexiconFile(
    val version: Int = 1,
    val categories: List<LexiconCategoryFile>
)

data class LexiconEntry(
    val categoryId: String,
    val keyword: String,
    val folded: String
)

class LexiconRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val entries: List<LexiconEntry> by lazy { load() }

    private fun load(): List<LexiconEntry> = try {
        val raw = context.assets.open("lexicon.json").bufferedReader().use { it.readText() }
        val file = json.decodeFromString(LexiconFile.serializer(), raw)
        file.categories.flatMap { category ->
            category.keywords
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .map { LexiconEntry(category.id, it, ArabicNormalizer.fold(it)) }
        }
    } catch (error: Throwable) {
        emptyList()
    }
}
