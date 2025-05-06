package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import com.tasnimulhasan.entity.home.MusicEntity
import javax.inject.Inject

class GetCurrentSongInfoUseCase @Inject constructor(
    private val repository: PlayerRepository
) {
    suspend operator fun invoke(): MusicEntity? {
        return repository.getCurrentSongInfo()
    }
}