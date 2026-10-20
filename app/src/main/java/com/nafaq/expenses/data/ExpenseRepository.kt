package com.nafaq.expenses.data

import com.nafaq.expenses.data.db.CategoryDao
import com.nafaq.expenses.data.db.CategoryEntity
import com.nafaq.expenses.data.db.ExpenseDao
import com.nafaq.expenses.data.db.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) {

    fun observeExpenses(): Flow<List<ExpenseEntity>> = expenseDao.observeAll()

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    suspend fun categories(): List<CategoryEntity> = categoryDao.getAll()

    suspend fun add(expense: ExpenseEntity): Long = expenseDao.insert(expense)

    suspend fun update(expense: ExpenseEntity) = expenseDao.update(expense)

    suspend fun remove(expense: ExpenseEntity) = expenseDao.delete(expense)

    suspend fun clearAll() = expenseDao.clear()

    suspend fun addCategory(category: CategoryEntity) = categoryDao.upsert(category)

    suspend fun categoryCount(): Int = categoryDao.count()

    suspend fun seedCategoriesIfEmpty() {
        if (categoryDao.count() == 0) {
            categoryDao.upsertAll(CategoryCatalog.DEFAULTS)
        }
    }
}
