package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import com.tasnimulhasan.entity.home.MusicEntity
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes the single, shared audio queue. Every screen that needs to show "what's in the
 * queue" should use this instead of independently re-fetching the library, so they can
 * never disagree with each other about ordering.
 */
class ObserveAudioListUseCase @Inject constructor(
    private val repository: PlayerRepository
) {
    operator fun invoke(): StateFlow<List<MusicEntity>> = repository.audioList
}
