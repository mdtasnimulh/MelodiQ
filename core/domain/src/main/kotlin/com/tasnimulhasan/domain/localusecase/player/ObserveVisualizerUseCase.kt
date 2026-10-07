package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveVisualizerUseCase @Inject constructor(
    private val repository: PlayerRepository
) {
    val bars: StateFlow<FloatArray> get() = repository.visualizerBars
    val isActive: StateFlow<Boolean> get() = repository.visualizerActive
}
