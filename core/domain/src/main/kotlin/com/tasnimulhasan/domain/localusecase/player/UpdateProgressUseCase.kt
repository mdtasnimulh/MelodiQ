package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import javax.inject.Inject

class UpdateProgressUseCase @Inject constructor(
    private val repo: PlayerRepository
) {
    suspend operator fun invoke(progress: Float) = repo.updateProgress(progress)
}