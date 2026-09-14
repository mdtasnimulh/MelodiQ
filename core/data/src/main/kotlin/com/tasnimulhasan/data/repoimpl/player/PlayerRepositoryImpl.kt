package com.tasnimulhasan.data.repoimpl.player

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tasnimulhasan.data.player.MelodiqAudioState
import com.tasnimulhasan.data.player.MelodiqPlayerEvent
import com.tasnimulhasan.data.player.MelodiqPlayerService
import com.tasnimulhasan.data.player.MelodiqServiceHandler
import com.tasnimulhasan.domain.localusecase.music.FetchMusicUseCase
import com.tasnimulhasan.domain.player.PlaybackSnapshot
import com.tasnimulhasan.domain.player.PlaybackState
import com.tasnimulhasan.domain.repository.PlayerRepository
import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val serviceHandler: MelodiqServiceHandler,
    private val fetchMusicUseCase: FetchMusicUseCase,
    private val preferencesDataStoreRepository: PreferencesDataStoreRepository,
    @ApplicationContext private val context: Context,
) : PlayerRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    private val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _audioList = MutableStateFlow<List<MusicEntity>>(emptyList())
    override val audioList: StateFlow<List<MusicEntity>> = _audioList.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)

    override val currentSelectedAudio: StateFlow<MusicEntity?> =
        combine(_audioList, _currentIndex) { list, index -> list.getOrNull(index) }
            .stateIn(repositoryScope, SharingStarted.Eagerly, null)

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var progressTickCount = 0

    init {
        repositoryScope.launch {
            serviceHandler.audioState.collect { state ->
                _playbackState.value = state.toDomain()
                when (state) {
                    is MelodiqAudioState.Progress -> {
                        // Throttle disk writes to ~every 5s while playing (10 ticks * 500ms),
                        // so a hard process kill mid-song still resumes close to where it left off.
                        progressTickCount++
                        if (progressTickCount % 10 == 0) persistCurrentPlaybackPosition()
                    }
                    is MelodiqAudioState.Playing -> {
                        _isPlaying.value = state.isPlaying
                        if (!state.isPlaying) persistCurrentPlaybackPosition()
                    }
                    is MelodiqAudioState.CurrentPlaying -> {
                        _currentIndex.value = state.mediaItemIndex
                        persistCurrentPlaybackPosition()
                    }
                    else -> Unit
                }
            }
        }

        repositoryScope.launch {
            preferencesDataStoreRepository.getSortType().collectLatest { sortType ->
                val sorted = fetchMusicUseCase(sortType)
                loadPlaylist(sorted, sortType)
            }
        }
    }

    private suspend fun persistCurrentPlaybackPosition() {
        val songId = serviceHandler.audioList.value
            .getOrNull(serviceHandler.getCurrentMediaItemIndex())?.songId ?: return
        preferencesDataStoreRepository.saveLastPlayedTrack(songId, serviceHandler.getCurrentDuration())
    }

    override suspend fun loadPlaylist(musicList: List<MusicEntity>, sortType: SortType, keepCurrentTrack: Boolean) {
        _audioList.value = musicList
        if (!keepCurrentTrack) {
            serviceHandler.updateMediaItems(musicList, sortType)
            _currentIndex.value = serviceHandler.getCurrentMediaItemIndex()
            return
        }
        if (serviceHandler.getMediaItemCount() == 0) {
            val lastPlayed = preferencesDataStoreRepository.getLastPlayedTrack()
            serviceHandler.updateMediaItemsWithCurrentTrack(
                audioList = musicList,
                sortType = sortType,
                restoreSongId = lastPlayed?.songId,
                restorePositionMs = lastPlayed?.positionMs ?: 0L,
            )
        } else {
            serviceHandler.updateMediaItemsWithCurrentTrack(musicList, sortType)
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

    override suspend fun getCurrentSongInfo(): MusicEntity? = currentSelectedAudio.value

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