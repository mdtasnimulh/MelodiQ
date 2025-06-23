package com.tasnimulhasan.domain.repository.local

import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.room.music.MelodiqEntity
import kotlinx.coroutines.flow.Flow

interface MelodiQRepository {

    suspend fun insertMusic(music: MelodiqEntity)

    suspend fun insertAllMusic(musicList: List<MelodiqEntity>)

    fun fetchAllMusic(sortType: SortType): Flow<List<MelodiqEntity>>

    suspend fun deleteMusicFromRoom(melodiqEntity: MelodiqEntity)

}