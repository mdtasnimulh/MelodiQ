package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import javax.inject.Inject

class FadeOutAndPauseUseCase @Inject constructor(
    private val repository: PlayerRepository
) {
    suspend operator fun invoke(durationMs: Long) = repository.fadeOutAndPause(durationMs)
}
