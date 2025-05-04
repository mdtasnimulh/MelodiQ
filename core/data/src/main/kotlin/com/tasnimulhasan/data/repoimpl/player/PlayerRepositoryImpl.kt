package com.tasnimulhasan.data.repoimpl.player

import com.tasnimulhasan.common.service.MelodiqAudioState
import com.tasnimulhasan.common.service.MelodiqPlayerEvent
import com.tasnimulhasan.common.service.MelodiqServiceHandler
import com.tasnimulhasan.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class PlayerRepositoryImpl @Inject constructor(
    private val serviceHandler: MelodiqServiceHandler
) : PlayerRepository {

    override suspend fun play() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.PlayPause)
    }

    override suspend fun pause() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.PlayPause)
    }

    override suspend fun seekTo(position: Long) {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.SeekTo, seekPosition = position)
    }

    override suspend fun next() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.SeekToNext)
    }

    override suspend fun previous() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.Backward)
    }

    override suspend fun getCurrentDuration(): Long {
        return serviceHandler.getCurrentDuration()
    }

    override suspend fun selectAudio(index: Int) {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.SelectAudioChange, selectedAudionIndex = index)
    }

    override suspend fun updateProgress(progress: Float) {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.UpdateProgress(progress))
    }

    override suspend fun observeAudioState(): StateFlow<MelodiqAudioState> = serviceHandler.audioState

}