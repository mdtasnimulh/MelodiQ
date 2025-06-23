package com.tasnimulhasan.domain.localusecase.melodiq

import com.tasnimulhasan.domain.localusecase.RoomCollectableUseCase
import com.tasnimulhasan.domain.repository.local.MelodiQRepository
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.room.music.MelodiqEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchRoomMusicUseCase @Inject constructor(
    private val repository: MelodiQRepository,
): RoomCollectableUseCase<SortType, List<MelodiqEntity>> {

    override fun invoke(sortType: SortType): Flow<List<MelodiqEntity>> {
        return repository.fetchAllMusic(sortType)
    }

}