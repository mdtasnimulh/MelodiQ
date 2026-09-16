package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import com.tasnimulhasan.entity.home.MusicEntity
import javax.inject.Inject

class PlayNextUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend operator fun invoke(song: MusicEntity) = repo.playNext(song)
}

class PlayLaterUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend operator fun invoke(song: MusicEntity) = repo.playLater(song)
}

class AddToQueueUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend operator fun invoke(songs: List<MusicEntity>) = repo.addToQueue(songs)
}

class RemoveFromQueueUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend operator fun invoke(index: Int) = repo.removeFromQueue(index)
}

class MoveQueueItemUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend operator fun invoke(from: Int, to: Int) = repo.moveQueueItem(from, to)
}

class ClearQueueUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend operator fun invoke() = repo.clearQueue()
}

class SetPlaybackSpeedUseCase @Inject constructor(private val repo: PlayerRepository) {
    suspend operator fun invoke(speed: Float) = repo.setPlaybackSpeed(speed)
    fun current(): Float = repo.getPlaybackSpeed()
}

class SeekStepUseCase @Inject constructor(private val repo: PlayerRepository) {
    operator fun invoke(stepMs: Long) = repo.setSeekStepMs(stepMs)
    fun current(): Long = repo.getSeekStepMs()
}
