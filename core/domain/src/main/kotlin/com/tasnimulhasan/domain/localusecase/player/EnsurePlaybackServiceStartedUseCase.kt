package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import javax.inject.Inject

class EnsurePlaybackServiceStartedUseCase @Inject constructor(
    private val repository: PlayerRepository
) {
    operator fun invoke() = repository.ensurePlaybackServiceStarted()
}