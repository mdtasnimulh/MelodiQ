package com.tasnimulhasan.domain.repository

import android.content.Context
import com.tasnimulhasan.entity.home.MusicEntity

interface MusicRepository {
    suspend fun fetchMusic(context: Context): List<MusicEntity>
}