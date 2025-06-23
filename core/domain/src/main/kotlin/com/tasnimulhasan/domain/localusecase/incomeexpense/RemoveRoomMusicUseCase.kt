package com.tasnimulhasan.domain.localusecase.incomeexpense

import com.tasnimulhasan.domain.localusecase.RoomSuspendableUseCaseNonReturn
import com.tasnimulhasan.domain.repository.local.MelodiQRepository
import com.tasnimulhasan.entity.room.music.MelodiqEntity
import javax.inject.Inject

class RemoveRoomMusicUseCase @Inject constructor(
    private val repository: MelodiQRepository
) : RoomSuspendableUseCaseNonReturn<RemoveRoomMusicUseCase.Params> {

    data class Params(
        val music: MelodiqEntity
    )

    override suspend fun invoke(params: Params) {
        return repository.deleteMusicFromRoom(params.music)
    }
}