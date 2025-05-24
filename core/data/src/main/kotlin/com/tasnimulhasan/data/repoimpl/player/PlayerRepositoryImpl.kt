package com.tasnimulhasan.data.repoimpl.player

import com.tasnimulhasan.common.service.MelodiqAudioState
import com.tasnimulhasan.common.service.MelodiqPlayerEvent
import com.tasnimulhasan.common.service.MelodiqServiceHandler
import com.tasnimulhasan.domain.localusecase.music.FetchMusicUseCase
import com.tasnimulhasan.domain.repository.PlayerRepository
import com.tasnimulhasan.entity.home.MusicEntity
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class PlayerRepositoryImpl @Inject constructor(
    private val serviceHandler: MelodiqServiceHandler,
    private val fetchMusicUseCase: FetchMusicUseCase,
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
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.SkipNext)
    }

    override suspend fun previous() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.SkipPrevious)
    }

    override suspend fun forward() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.ForwardTrack5Sec)
    }

    override suspend fun backward() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.BackwardTrack5Sec)
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

    override suspend fun getCurrentSongInfo(): MusicEntity? {
        val currentIndex = serviceHandler.audioState.value.let { state ->
            if (state is MelodiqAudioState.CurrentPlaying) state.mediaItemIndex else -1
        }
        return if (currentIndex >= 0) {
            val audioLIst = fetchMusicUseCase.execute()
            audioLIst.getOrNull(currentIndex)
        } else {
            null
        }
    }

    override suspend fun repeatTrackOne() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.RepeatTrackOne)
    }

    override suspend fun repeatTrackAll() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.RepeatTrackALl)
    }

    override suspend fun repeatTrackOff() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.RepeatTrackOff)
    }
}