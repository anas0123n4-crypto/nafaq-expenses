package com.nafaq.expenses.data.parser

object NumberWords {

    private val values: Map<String, Double> = mapOf(
        "صفر" to 0.0,
        "نص" to 0.5,
        "نصف" to 0.5,
        "ونص" to 0.5,
        "ونصف" to 0.5,
        "ربع" to 0.25,
        "واحد" to 1.0,
        "واحده" to 1.0,
        "وحده" to 1.0,
        "اثنان" to 2.0,
        "اثنين" to 2.0,
        "ثنين" to 2.0,
        "ثاني" to 2.0,
        "ثلاث" to 3.0,
        "ثلاثه" to 3.0,
        "اربع" to 4.0,
        "اربعه" to 4.0,
        "خمس" to 5.0,
        "خمسه" to 5.0,
        "ست" to 6.0,
        "سته" to 6.0,
        "سبع" to 7.0,
        "سبعه" to 7.0,
        "ثمان" to 8.0,
        "ثمانيه" to 8.0,
        "تسع" to 9.0,
        "تسعه" to 9.0,
        "عشر" to 10.0,
        "عشره" to 10.0,
        "عشرين" to 20.0,
        "عشرون" to 20.0,
        "ثلاثين" to 30.0,
        "ثلاثون" to 30.0,
        "اربعين" to 40.0,
        "اربعون" to 40.0,
        "خمسين" to 50.0,
        "خمسون" to 50.0,
        "ستين" to 60.0,
        "ستون" to 60.0,
        "سبعين" to 70.0,
        "سبعون" to 70.0,
        "ثمانون" to 80.0,
        "ثمانين" to 80.0,
        "تسعين" to 90.0,
        "تسعون" to 90.0,
        "مئه" to 100.0,
        "مائه" to 100.0,
        "مئة" to 100.0,
        "مائتين" to 200.0,
        "مئتين" to 200.0,
        "الف" to 1000.0,
        "الاف" to 1000.0,
        "الفان" to 2000.0,
        "الفين" to 3000.0
    )

    fun value(word: String): Double? {
        val cleaned = word.trim()
        if (cleaned.isEmpty()) return null
        values[cleaned]?.let { return it }
        if (cleaned.length > 1) {
            val first = cleaned[0]
            if (first == 'و' || first == 'ب') {
                values[cleaned.substring(1)]?.let { return it }
            }
        }
        return null
    }
}
