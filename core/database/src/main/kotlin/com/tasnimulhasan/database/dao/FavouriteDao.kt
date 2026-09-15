package com.tasnimulhasan.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasnimulhasan.entity.room.favourite.FavouriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavourite(item: FavouriteEntity)

    @Query("DELETE FROM melodiq_favourite_table WHERE songId = :songId")
    suspend fun removeFavourite(songId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM melodiq_favourite_table WHERE songId = :songId)")
    suspend fun isFavourite(songId: Long): Boolean

    @Query("SELECT songId FROM melodiq_favourite_table")
    fun getFavouriteIds(): Flow<List<Long>>
}
