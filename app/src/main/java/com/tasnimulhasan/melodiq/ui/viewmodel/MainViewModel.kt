package com.tasnimulhasan.melodiq.ui.viewmodel

import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.datastore.GetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.music.FetchMusicUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.player.PlaybackState
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val fetchMusicUseCase: FetchMusicUseCase,
    private val playerUseCases: PlayerUseCases,
    private val getSortTypeUseCase: GetSortTypeUseCase,
) : BaseViewModel() {
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

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentSelectedAudio = MutableStateFlow(dummyAudio)
    val currentSelectedAudio = _currentSelectedAudio.asStateFlow()

    private val _audioList = MutableStateFlow(listOf<MusicEntity>())
    val audioList: StateFlow<List<MusicEntity>> = _audioList.asStateFlow()

    private val _uIState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uIState: StateFlow<UiState> = _uIState.asStateFlow()

    init {
        viewModelScope.launch {
            getSortTypeUseCase().collectLatest { persistedSortType ->
                _sortType.value = persistedSortType
                val sorted = fetchMusicUseCase(persistedSortType)
                _audioList.value = sorted
                _uIState.value = UiState.MusicList(sorted)
                playerUseCases.loadPlaylist(sorted, persistedSortType)
                restorePlaybackState()
            }
        }

        viewModelScope.launch {
            playerUseCases.observeAudioState().collectLatest { mediaState ->
                when (mediaState) {
                    PlaybackState.Idle -> _uIState.value = UiState.Initial
                    is PlaybackState.Buffering -> calculateProgressValue(mediaState.position)
                    is PlaybackState.Playing -> _isPlaying.value = mediaState.isPlaying
                    is PlaybackState.Progress -> calculateProgressValue(mediaState.position)
                    is PlaybackState.TrackChanged -> {
                        _currentSelectedAudio.value = _audioList.value.getOrNull(mediaState.index) ?: dummyAudio
                    }
                    is PlaybackState.Ready -> {
                        _duration.value = mediaState.duration
                        _uIState.value = UiState.Ready
                        calculateProgressValue(playerUseCases.getPlaybackSnapshot().position)
                    }
                }
            }
        }
    }

    private suspend fun restorePlaybackState() {
        val snapshot = playerUseCases.getPlaybackSnapshot()
        _currentSelectedAudio.value = _audioList.value.getOrNull(snapshot.currentIndex) ?: dummyAudio
        _duration.value = snapshot.duration
        calculateProgressValue(snapshot.position)
        _isPlaying.value = snapshot.isPlaying
    }

    fun onUiEvents(uiEvents: UiEvent) = viewModelScope.launch {
        when (uiEvents) {
            is UiEvent.Backward -> playerUseCases.backwardTrackUseCase()
            is UiEvent.Forward -> playerUseCases.forwardTrackUseCase()
            is UiEvent.PlayPause -> {
                if (_isPlaying.value) playerUseCases.pause()
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
        }
    }

    private fun calculateProgressValue(currentProgress: Long) {
        _progress.value =
            if (currentProgress > 0 && _duration.value > 0) ((currentProgress.toFloat() / _duration.value.toFloat()) * 100f)
            else 0f
        _progressString.value = formatDuration(currentProgress)
    }

    private fun formatDuration(duration: Long): String {
        val minute = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minute, seconds)
    }

    fun convertLongToReadableDateTime(time: Long, format: String): String {
        val df = SimpleDateFormat(format, Locale.US)
        return df.format(time)
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
    data class UpdateProgress(val newProgress: Float) : UiEvent()
}

sealed class UiState {
    data class MusicList(val musics: List<MusicEntity>) : UiState()
    data object Initial : UiState()
    data object Ready : UiState()
}