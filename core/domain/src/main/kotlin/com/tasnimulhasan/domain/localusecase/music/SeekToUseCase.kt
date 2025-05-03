package com.tasnimulhasan.domain.localusecase.music

import com.tasnimulhasan.domain.repository.PlayerRepository

class SeekToUseCase(private val playerRepository: PlayerRepository) {
    operator fun invoke(positionMs: Long) = playerRepository.seekTo(positionMs)
}