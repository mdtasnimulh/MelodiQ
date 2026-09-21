package com.tasnimulhasan.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasnimulhasan.entity.room.lyrics.LyricsCacheEntity

@Dao
interface LyricsCacheDao {
    @Query("SELECT * FROM lyrics_cache_table WHERE songId = :songId LIMIT 1")
    suspend fun get(songId: Long): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LyricsCacheEntity)
}
