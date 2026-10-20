package com.nafaq.expenses.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val categoryId: String,
    val amount: Double,
    val currency: String,
    val rawText: String? = null,
    val note: String? = null,
    val createdAt: Long,
    val source: String = SOURCE_VOICE
) {
    companion object {
        const val SOURCE_VOICE = "voice"
        const val SOURCE_MANUAL = "manual"
    }
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val colorHex: String,
    val emoji: String,
    val sortOrder: Int = 0,
    val isCustom: Boolean = false
)
