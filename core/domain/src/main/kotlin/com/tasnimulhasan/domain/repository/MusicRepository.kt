package com.tasnimulhasan.domain.repository

import android.content.Context
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    suspend fun fetchMusic(context: Context, sortType: SortType): List<MusicEntity>
}