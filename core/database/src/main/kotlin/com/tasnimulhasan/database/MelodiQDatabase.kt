package com.tasnimulhasan.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tasnimulhasan.database.dao.MelodiQDao
import com.tasnimulhasan.entity.room.incomeexpense.MelodiQEntity

@Database(
    entities = [MelodiQEntity::class],
    version = 1,
    exportSchema = true
)
internal abstract class MelodiQDatabase : RoomDatabase() {
    abstract fun melodiQDao(): MelodiQDao
}