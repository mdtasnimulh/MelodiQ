package com.tasnimulhasan.domain.localusecase.melodiq

import com.tasnimulhasan.domain.localusecase.RoomSuspendableUseCaseNonReturn
import com.tasnimulhasan.domain.repository.local.MelodiQRepository
import com.tasnimulhasan.entity.room.music.MelodiqEntity
import javax.inject.Inject

class InsertRoomMusicListUseCase @Inject constructor(
    private val repository: MelodiQRepository
) : RoomSuspendableUseCaseNonReturn<InsertRoomMusicListUseCase.Params> {

    data class Params(
        val musicList: List<MelodiqEntity>
    )

    override suspend fun invoke(params: Params) {
        return repository.insertAllMusic(params.musicList)
    }
}