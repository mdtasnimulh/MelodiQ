package com.tasnimulhasan.domain.localusecase.music

import com.tasnimulhasan.domain.repository.PlayerRepository

class GetCurrentDurationUseCase(private val playerRepository: PlayerRepository) {
    operator fun invoke(): Long = playerRepository.getCurrentPosition()
}