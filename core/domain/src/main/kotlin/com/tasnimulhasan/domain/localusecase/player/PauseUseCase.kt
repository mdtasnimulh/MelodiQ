package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import javax.inject.Inject

class PauseUseCase @Inject constructor(
    private val repo: PlayerRepository
) {
    suspend operator fun invoke() = repo.pause()
}