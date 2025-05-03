package com.tasnimulhasan.domain.localusecase.music

import com.tasnimulhasan.domain.repository.PlayerRepository

class PreviousTrackUseCase(private val playerRepository: PlayerRepository) {
    operator fun invoke() = playerRepository.skipToPrevious()
}