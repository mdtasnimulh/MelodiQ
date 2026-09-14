package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes the single, shared "is something currently playing" flag.
 */
class ObserveIsPlayingUseCase @Inject constructor(
    private val repository: PlayerRepository
) {
    operator fun invoke(): StateFlow<Boolean> = repository.isPlaying
}
