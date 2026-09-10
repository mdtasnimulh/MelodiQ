package com.tasnimulhasan.data.repoimpl.player

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tasnimulhasan.data.player.MelodiqAudioState
import com.tasnimulhasan.data.player.MelodiqPlayerEvent
import com.tasnimulhasan.data.player.MelodiqPlayerService
import com.tasnimulhasan.data.player.MelodiqServiceHandler
import com.tasnimulhasan.domain.localusecase.datastore.GetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.music.FetchMusicUseCase
import com.tasnimulhasan.domain.player.PlaybackSnapshot
import com.tasnimulhasan.domain.player.PlaybackState
import com.tasnimulhasan.domain.repository.PlayerRepository
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val serviceHandler: MelodiqServiceHandler,
    private val fetchMusicUseCase: FetchMusicUseCase,
    private val getSortTypeUseCase: GetSortTypeUseCase,
    @ApplicationContext private val context: Context,
) : PlayerRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    private val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        repositoryScope.launch {
            serviceHandler.audioState.collect { _playbackState.value = it.toDomain() }
        }
    }

    override suspend fun loadPlaylist(musicList: List<MusicEntity>, sortType: SortType, keepCurrentTrack: Boolean) {
        if (keepCurrentTrack) {
            serviceHandler.updateMediaItemsWithCurrentTrack(musicList, sortType)
        } else {
            serviceHandler.updateMediaItems(musicList, sortType)
        }
    }

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

    override suspend fun getCurrentDuration(): Long = serviceHandler.getCurrentDuration()

    override suspend fun selectAudio(index: Int) {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.SelectAudioChange, selectedAudionIndex = index)
    }

    override suspend fun updateProgress(progress: Float) {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.UpdateProgress(progress))
    }

    override suspend fun observeAudioState(): StateFlow<PlaybackState> = playbackState

    override suspend fun getCurrentSongInfo(): MusicEntity? {
        val currentIndex = serviceHandler.audioState.value.let { state ->
            if (state is MelodiqAudioState.CurrentPlaying) state.mediaItemIndex else -1
        }
        return if (currentIndex >= 0) {
            fetchMusicUseCase(SortType.DATE_MODIFIED_DESC).getOrNull(currentIndex)
        } else {
            null
        }
    }

    override suspend fun getPlaybackSnapshot(): PlaybackSnapshot = PlaybackSnapshot(
        currentIndex = serviceHandler.getCurrentMediaItemIndex(),
        duration = serviceHandler.getDuration(),
        position = serviceHandler.getCurrentDuration(),
        isPlaying = serviceHandler.isPlaying(),
        mediaItemCount = serviceHandler.getMediaItemCount(),
        sortType = serviceHandler.sortType.value,
    )

    override fun isPlaybackServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == MelodiqPlayerService::class.java.name }
    }

    override fun ensurePlaybackServiceStarted() {
        if (!isPlaybackServiceRunning()) {
            val intent = Intent(context, MelodiqPlayerService::class.java)
            ContextCompat.startForegroundService(context, intent)
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

private fun MelodiqAudioState.toDomain(): PlaybackState = when (this) {
    MelodiqAudioState.Initial -> PlaybackState.Idle
    is MelodiqAudioState.Ready -> PlaybackState.Ready(duration)
    is MelodiqAudioState.Progress -> PlaybackState.Progress(progress)
    is MelodiqAudioState.Buffering -> PlaybackState.Buffering(progress)
    is MelodiqAudioState.Playing -> PlaybackState.Playing(isPlaying)
    is MelodiqAudioState.CurrentPlaying -> PlaybackState.TrackChanged(mediaItemIndex)
}