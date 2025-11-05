package com.tasnimulhasan.entity.room.playlist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_details_table")
data class PlaylistDetailsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val playlistId: Int,
    val contentUri: String,
    val songId: Long,
    val cover: String,
    val songTitle: String,
    val artist: String?,
    val duration: String,
    val album: String?,
    val albumId: Long?
)
