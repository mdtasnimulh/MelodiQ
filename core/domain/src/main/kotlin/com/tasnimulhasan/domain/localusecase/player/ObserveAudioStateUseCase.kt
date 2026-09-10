package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.player.PlaybackState
import com.tasnimulhasan.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveAudioStateUseCase @Inject constructor(
    private val repository: PlayerRepository
) {
    suspend operator fun invoke(): StateFlow<PlaybackState> = repository.observeAudioState()
}