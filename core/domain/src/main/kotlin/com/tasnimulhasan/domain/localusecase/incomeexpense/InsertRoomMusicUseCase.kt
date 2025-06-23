package com.tasnimulhasan.domain.localusecase.incomeexpense

import com.tasnimulhasan.domain.localusecase.RoomSuspendableUseCaseNonReturn
import com.tasnimulhasan.domain.repository.local.MelodiQRepository
import com.tasnimulhasan.entity.room.music.MelodiqEntity
import javax.inject.Inject

class InsertRoomMusicUseCase @Inject constructor(
    private val repository: MelodiQRepository
) : RoomSuspendableUseCaseNonReturn<InsertRoomMusicUseCase.Params> {

    data class Params(
        val melodiqEntity: MelodiqEntity
    )

    override suspend fun invoke(params: Params) {
        return repository.insertMusic(params.melodiqEntity)
    }
}