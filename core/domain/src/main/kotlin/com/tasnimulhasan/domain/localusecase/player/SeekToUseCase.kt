package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import javax.inject.Inject

class SeekToUseCase @Inject constructor(
    private val repo: PlayerRepository
) {
    suspend operator fun invoke(position: Long) = repo.seekTo(position)
}