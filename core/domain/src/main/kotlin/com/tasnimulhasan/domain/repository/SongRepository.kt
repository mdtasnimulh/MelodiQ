package com.tasnimulhasan.domain.repository

import com.tasnimulhasan.common.Resource
import com.tasnimulhasan.entity.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getSongs(): Flow<Resource<List<Song>>>
}