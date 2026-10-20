package com.nafaq.expenses.data.ai

data class AiModel(
    val id: String,
    val label: String
)

data class AiSuggestion(
    val itemName: String?,
    val categoryId: String?,
    val amount: Double?,
    val currency: String?,
    val note: String?
)

object AiProviders {
    const val GEMINI = "gemini"
    const val OPENROUTER = "openrouter"
    const val CUSTOM = "custom"

    fun label(id: String): String = when (id) {
        GEMINI -> "Google Gemini"
        OPENROUTER -> "OpenRouter"
        CUSTOM -> "مزوّد مخصص (OpenAI)"
        else -> id
    }
}
