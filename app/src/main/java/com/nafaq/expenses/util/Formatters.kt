package com.nafaq.expenses.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {

    private val amountFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

    private val weekdayNames = arrayOf(
        "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    )

    private val monthNames = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    fun amount(value: Double): String = amountFormat.format(value)

    fun time(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        val period = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "ص" else "م"
        return String.format(Locale.US, "%d:%02d %s", hour, minute, period)
    }

    fun shortDate(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        return "$day/$month"
    }

    fun fullDate(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        return "$day/$month/$year"
    }

    fun dayLabel(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val weekday = weekdayNames[(calendar.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
        return "$weekday $day/$month"
    }

    fun monthLabel(year: Int, monthIndex: Int): String = "${monthNames[monthIndex.coerceIn(0, 11)]} $year"

    fun monthLabel(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        return monthLabel(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
    }

    fun isSameDay(first: Long, second: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = first
        val firstDay = calendar.get(Calendar.DAY_OF_YEAR)
        val firstYear = calendar.get(Calendar.YEAR)
        calendar.timeInMillis = second
        return firstDay == calendar.get(Calendar.DAY_OF_YEAR) && firstYear == calendar.get(Calendar.YEAR)
    }

    fun isToday(millis: Long): Boolean = isSameDay(millis, System.currentTimeMillis())

    fun now(): Date = Date()
}
