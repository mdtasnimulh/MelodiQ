package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.common.service.MelodiqAudioState
import com.tasnimulhasan.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveAudioStateUseCase @Inject constructor(
    private val repository: PlayerRepository
) {
    suspend operator fun invoke(): StateFlow<MelodiqAudioState> = repository.observeAudioState()
}
