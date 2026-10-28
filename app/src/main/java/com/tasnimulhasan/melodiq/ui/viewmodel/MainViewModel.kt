package com.tasnimulhasan.melodiq.ui.viewmodel

import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.datastore.GetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetVisualizerStyleUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.player.PlaybackState
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.enums.VisualizerStyle
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playerUseCases: PlayerUseCases,
    private val getSortTypeUseCase: GetSortTypeUseCase,
    private val getVisualizerStyleUseCase: GetVisualizerStyleUseCase,
    getMiniPlayerPositionUseCase: com.tasnimulhasan.domain.localusecase.datastore.GetMiniPlayerPositionUseCase,
    private val setMiniPlayerPositionUseCase: com.tasnimulhasan.domain.localusecase.datastore.SetMiniPlayerPositionUseCase,
) : BaseViewModel() {
    val miniPlayerPosition: StateFlow<com.tasnimulhasan.entity.enums.MiniPlayerPosition> = getMiniPlayerPositionUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.tasnimulhasan.entity.enums.MiniPlayerPosition.BOTTOM_END)

    fun setMiniPlayerPosition(position: com.tasnimulhasan.entity.enums.MiniPlayerPosition) {
        viewModelScope.launch { setMiniPlayerPositionUseCase(position) }
    }

    val visualizerStyle: StateFlow<VisualizerStyle> = getVisualizerStyleUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VisualizerStyle.OFF)
    val visualizerBars: StateFlow<FloatArray> = playerUseCases.observeVisualizer.bars

    private val dummyAudio = MusicEntity(
        contentUri = "".toUri(),
        songId = 0L,
        cover = null,
        songTitle = "",
        artist = "",
        duration = "",
        albumId = 0L,
        album = ""
    )

    private val _sortType = MutableStateFlow(SortType.DATE_MODIFIED_DESC)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _progressString = MutableStateFlow("00:00")
    val progressString = _progressString.asStateFlow()

    // Bound directly to the shared play/pause state rather than a local copy fed by
    // PlaybackState.Playing - that signal travelled on a conflated StateFlow and could be
    // dropped, leaving the play/pause button out of sync with reality.
    val isPlaying: StateFlow<Boolean> = playerUseCases.observeIsPlaying()

    // Both of these are pass-throughs onto the repository's single shared StateFlow -
    // never re-fetched or re-derived here, so this screen can never disagree with the
    // full player or the song list about what's currently loaded/playing.
    val audioList: StateFlow<List<MusicEntity>> = playerUseCases.observeAudioList()

    val currentSelectedAudio: StateFlow<MusicEntity> = playerUseCases.observeCurrentSelectedAudio()
        .map { it ?: dummyAudio }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), dummyAudio)

    private val _uIState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uIState: StateFlow<UiState> = _uIState.asStateFlow()

    init {
        viewModelScope.launch {
            getSortTypeUseCase().collectLatest { persistedSortType ->
                _sortType.value = persistedSortType
            }
        }

        viewModelScope.launch {
            playerUseCases.observeAudioState().collectLatest { mediaState ->
                when (mediaState) {
                    PlaybackState.Idle -> _uIState.value = UiState.Initial
                    is PlaybackState.Buffering -> calculateProgressValue(mediaState.position)
                    is PlaybackState.Playing -> Unit
                    is PlaybackState.Progress -> calculateProgressValue(mediaState.position)
                    is PlaybackState.TrackChanged -> Unit
                    is PlaybackState.Ready -> {
                        _duration.value = mediaState.duration
                        _uIState.value = UiState.Ready
                        calculateProgressValue(playerUseCases.getPlaybackSnapshot().position)
                    }
                }
            }
        }

        restorePlaybackState()
    }

    private fun restorePlaybackState() = viewModelScope.launch {
        val snapshot = playerUseCases.getPlaybackSnapshot()
        _duration.value = snapshot.duration
        calculateProgressValue(snapshot.position)
    }

    fun onUiEvents(uiEvents: UiEvent) = viewModelScope.launch {
        when (uiEvents) {
            is UiEvent.Backward -> playerUseCases.backwardTrackUseCase()
            is UiEvent.Forward -> playerUseCases.forwardTrackUseCase()
            is UiEvent.PlayPause -> {
                if (isPlaying.value) playerUseCases.pause()
                else playerUseCases.play()
            }
            is UiEvent.SeekTo -> {
                val position = ((_duration.value * uiEvents.position) / 100f).toLong()
                playerUseCases.seekTo(position)
            }
            UiEvent.SeekToNext -> playerUseCases.next()
            is UiEvent.SelectedAudioChange -> playerUseCases.selectAudioChange(uiEvents.index)
            is UiEvent.UpdateProgress -> playerUseCases.updateProgress(uiEvents.newProgress)
            UiEvent.SeekToPrevious -> playerUseCases.previous()
            UiEvent.StopPlayback -> playerUseCases.stopPlayback()
        }
    }

    private fun calculateProgressValue(currentProgress: Long) {
        _progress.value =
            if (currentProgress > 0 && _duration.value > 0) ((currentProgress.toFloat() / _duration.value.toFloat()) * 100f)
            else 0f
        _progressString.value = formatDuration(currentProgress)
    }

    private fun formatDuration(duration: Long): String {
        val total = (duration / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
            else String.format(java.util.Locale.US, "%02d:%02d", m, s)
    }

    fun convertLongToReadableDateTime(time: Long, format: String): String {
        if (format != "mm:ss") return SimpleDateFormat(format, Locale.US).format(time)
        // Hours are shown once a track passes 60 minutes (1:55:20 instead of 55:20).
        val total = (time / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
            else String.format(java.util.Locale.US, "%02d:%02d", m, s)
    }

    fun isPlaybackServiceRunning(): Boolean = playerUseCases.isPlaybackServiceRunning()
    fun ensurePlaybackServiceStarted() = playerUseCases.ensurePlaybackServiceStarted()
}

sealed class UiEvent {
    data object PlayPause : UiEvent()
    data class SelectedAudioChange(val index: Int) : UiEvent()
    data class SeekTo(val position: Float) : UiEvent()
    data object SeekToNext : UiEvent()
    data object SeekToPrevious : UiEvent()
    data object Backward : UiEvent()
    data object Forward : UiEvent()
    data object StopPlayback : UiEvent()
    data class UpdateProgress(val newProgress: Float) : UiEvent()
}

sealed class UiState {
    data class MusicList(val musics: List<MusicEntity>) : UiState()
    data object Initial : UiState()
    data object Ready : UiState()
}