package com.tasnimulhasan.data.repoimpl.local

import com.tasnimulhasan.database.dao.MelodiQDao
import com.tasnimulhasan.domain.repository.local.MelodiQRepository
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.room.music.MelodiqEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MelodiQRepoImpl @Inject constructor(
    private val melodiQDao: MelodiQDao
) : MelodiQRepository {

    override suspend fun insertMusic(music: MelodiqEntity) {
        melodiQDao.insertMusic(music = music)
    }

    override suspend fun insertAllMusic(musicList: List<MelodiqEntity>) {
        melodiQDao.insertAllMusic(musicList = musicList)
    }

    override fun fetchAllMusic(sortType: SortType): Flow<List<MelodiqEntity>> {
        return melodiQDao.fetchAllMusic(sortType = sortType)
    }

    override suspend fun deleteMusicFromRoom(melodiqEntity: MelodiqEntity) {
        melodiQDao.deleteMusicFromRoom(melodiqEntity = melodiqEntity)
    }

}