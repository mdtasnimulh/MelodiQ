package com.tasnimulhasan.domain.localusecase.music

import com.tasnimulhasan.domain.repository.PlayerRepository

class NextTrackUseCase(private val playerRepository: PlayerRepository) {
    operator fun invoke() = playerRepository.skipToNext()
}