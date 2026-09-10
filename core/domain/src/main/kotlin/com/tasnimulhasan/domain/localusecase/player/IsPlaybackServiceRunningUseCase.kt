package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import javax.inject.Inject

class IsPlaybackServiceRunningUseCase @Inject constructor(
    private val repository: PlayerRepository
) {
    operator fun invoke(): Boolean = repository.isPlaybackServiceRunning()
}