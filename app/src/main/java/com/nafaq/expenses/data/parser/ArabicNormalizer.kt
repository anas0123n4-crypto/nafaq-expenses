package com.nafaq.expenses.data.parser

object ArabicNormalizer {

    private val tashkeelRegex = Regex("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED]")
    private val spaceRegex = Regex("\\s+")

    fun normalize(raw: String): String {
        var text = raw
        text = tashkeelRegex.replace(text, "")
        text = text.replace("\u0640", "")
        text = translateDigits(text)
        text = text.replace("\u200B", "")
        text = spaceRegex.replace(text, " ").trim()
        return text
    }

    fun fold(raw: String): String {
        var text = normalize(raw)
        text = text
            .replace("\u0623", "\u0627")
            .replace("\u0625", "\u0627")
            .replace("\u0622", "\u0627")
            .replace("\u0671", "\u0627")
            .replace("\u0649", "\u064A")
            .replace("\u0626", "\u064A")
            .replace("\u0624", "\u0648")
            .replace("\u0629", "\u0647")
            .lowercase()
        return text
    }

    fun translateDigits(text: String): String {
        val builder = StringBuilder(text.length)
        for (character in text) {
            builder.append(
                when {
                    character in '\u0660'..'\u0669' -> '0' + (character - '\u0660')
                    character in '\u06F0'..'\u06F9' -> '0' + (character - '\u06F0')
                    else -> character
                }
            )
        }
        return builder.toString()
    }
}
