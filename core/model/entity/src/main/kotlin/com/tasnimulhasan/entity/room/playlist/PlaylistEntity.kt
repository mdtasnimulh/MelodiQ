package com.tasnimulhasan.entity.room.playlist

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName

@Entity(tableName = "melodiq_playlist_table")
data class PlaylistEntity(
    @SerialName("id")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @SerialName("playlistName")
    val playlistName: String,

    @SerialName("playlistDescription")
    val playlistDescription: String,

    @SerialName("createdAt")
    val createdAt: Long,
)
