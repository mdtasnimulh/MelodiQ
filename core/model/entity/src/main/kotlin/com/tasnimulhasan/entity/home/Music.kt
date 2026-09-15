package com.tasnimulhasan.entity.home

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MusicEntity (
    val contentUri: Uri,
    val songId: Long,
    val cover: Bitmap?,
    val songTitle: String,
    val artist: String,
    val duration: String,
    val album: String,
    val albumId: Long,
    // MediaStore's DATE_ADDED (epoch seconds), cached alongside everything else so the
    // library can be re-sorted in memory without a second MediaStore query.
    val dateAdded: Long = 0L,
): Parcelable