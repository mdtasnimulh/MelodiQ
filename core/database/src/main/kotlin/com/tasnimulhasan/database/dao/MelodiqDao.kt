package com.tasnimulhasan.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.room.music.MelodiqEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MelodiQDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMusic(music: MelodiqEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMusic(musicList: List<MelodiqEntity>)

    @Query("SELECT * FROM melodiq_music_table ORDER BY:sortType")
    fun fetchAllMusic(sortType: SortType): Flow<List<MelodiqEntity>>

    @Delete()
    suspend fun deleteMusicFromRoom(melodiqEntity: MelodiqEntity)

}