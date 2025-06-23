package com.tasnimulhasan.entity.room.music

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "melodiq_music_table")
data class MelodiqEntity(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "musicId")
    val musicId: Long,
    @ColumnInfo(name = "musicTitle")
    val musicTitle: String,
    @ColumnInfo(name = "musicArtist")
    val musicArtist: String,
    @ColumnInfo(name = "musicPath")
    val musicPath: String,
    @ColumnInfo(name = "musicCover")
    val musicCover: Bitmap,
    @ColumnInfo(name = "musicDuration")
    val musicDuration: Long,
    @ColumnInfo(name = "album")
    val album: String,
    @ColumnInfo(name = "albumId")
    val albumId: Long,

)
