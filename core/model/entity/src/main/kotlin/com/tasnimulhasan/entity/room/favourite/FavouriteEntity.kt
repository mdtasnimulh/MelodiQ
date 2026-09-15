package com.tasnimulhasan.entity.room.favourite

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "melodiq_favourite_table")
data class FavouriteEntity(
    @PrimaryKey
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis(),
)
