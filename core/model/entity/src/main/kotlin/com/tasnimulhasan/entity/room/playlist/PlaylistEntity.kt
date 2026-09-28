package com.tasnimulhasan.entity.room.playlist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "melodiq_playlist_table")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val playlistName: String,

    val playlistDescription: String,

    val createdAt: Long,
)