package com.tasnimulhasan.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tasnimulhasan.database.dao.IncomeExpenseDao
import com.tasnimulhasan.entity.room.incomeexpense.IncomeExpenseEntity

@Database(
    entities = [IncomeExpenseEntity::class],
    version = 1,
    exportSchema = true
)
internal abstract class MmDatabase : RoomDatabase() {
    abstract fun incomeExpenseDao(): IncomeExpenseDao
}