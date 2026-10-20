package com.nafaq.expenses.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NafaqDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        fun build(context: Context): NafaqDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NafaqDatabase::class.java,
                "nafaq.db"
            ).fallbackToDestructiveMigration().build()
    }
}
