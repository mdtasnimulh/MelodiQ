package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import javax.inject.Inject

class SelectAudioChangeUseCase @Inject constructor(
    private val repo: PlayerRepository
) {
    suspend operator fun invoke(index: Int) = repo.selectAudio(index)
}