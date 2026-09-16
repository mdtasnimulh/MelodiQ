package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import com.tasnimulhasan.entity.home.MusicEntity
import javax.inject.Inject

class PlayCuratedQueueUseCase @Inject constructor(
    private val repo: PlayerRepository
) {
    suspend operator fun invoke(musicList: List<MusicEntity>, startIndex: Int) =
        repo.playCuratedQueue(musicList, startIndex)
}
