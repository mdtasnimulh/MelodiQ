package com.tasnimulhasan.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tasnimulhasan.database.dao.MelodiQDao
import com.tasnimulhasan.database.dao.PlaylistDetailsDao
import com.tasnimulhasan.database.dao.PlaylistsDao
import com.tasnimulhasan.entity.room.music.MelodiQEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity

@Database(
    entities = [
        MelodiQEntity::class,
        PlaylistEntity::class,
        PlaylistDetailsEntity::class,
    ],
    version = 1,
    exportSchema = false
)
internal abstract class MelodiQDatabase : RoomDatabase() {
    abstract fun melodiQDao(): MelodiQDao

    abstract fun playlistsDao(): PlaylistsDao

    abstract fun playlistDetailsDao(): PlaylistDetailsDao
}