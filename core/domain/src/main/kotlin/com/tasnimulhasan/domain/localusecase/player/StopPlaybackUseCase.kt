package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import javax.inject.Inject

/** Fully ends the listening session: stops audio, removes the notification, forgets "last played". */
class StopPlaybackUseCase @Inject constructor(
    private val repo: PlayerRepository
) {
    suspend operator fun invoke() = repo.stopPlayback()
}
