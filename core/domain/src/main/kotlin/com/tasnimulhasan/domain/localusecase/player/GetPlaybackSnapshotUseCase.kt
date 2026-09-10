package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.player.PlaybackSnapshot
import com.tasnimulhasan.domain.repository.PlayerRepository
import javax.inject.Inject

class GetPlaybackSnapshotUseCase @Inject constructor(
    private val repo: PlayerRepository
) {
    suspend operator fun invoke(): PlaybackSnapshot = repo.getPlaybackSnapshot()
}