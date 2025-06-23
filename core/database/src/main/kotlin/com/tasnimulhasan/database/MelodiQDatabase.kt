package com.tasnimulhasan.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tasnimulhasan.database.dao.MelodiQDao
import com.tasnimulhasan.entity.room.BitmapConverter
import com.tasnimulhasan.entity.room.music.MelodiqEntity

@Database(
    entities = [MelodiqEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(
    BitmapConverter::class
)
internal abstract class MelodiQDatabase : RoomDatabase() {
    abstract fun melodiQDao(): MelodiQDao
}