package com.tasnimulhasan.melodiq.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.tasnimulhasan.common.service.MelodiqAudioState
import com.tasnimulhasan.common.service.MelodiqPlayerEvent
import com.tasnimulhasan.common.service.MelodiqServiceHandler
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.datastore.GetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.music.FetchMusicUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val fetchMusicUseCase: FetchMusicUseCase,
    private val playerUseCases: PlayerUseCases,
    private val audioServiceHandler: MelodiqServiceHandler,
    private val getSortTypeUseCase: GetSortTypeUseCase,
    private val setSortTypeUseCase: SetSortTypeUseCase,
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

    private val _sortType = MutableStateFlow(audioServiceHandler.sortType.value)
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
            getSortTypeUseCase().collect {
                _sortType.value = it
                audioServiceHandler.sortType.value = _sortType.value
            }
        }

        initializeListIfNeeded()

        viewModelScope.launch {
            audioServiceHandler.audioState.collectLatest { mediaState ->
                when (mediaState) {
                    MelodiqAudioState.Initial -> _uIState.value = UiState.Initial
                    is MelodiqAudioState.Buffering -> calculateProgressValue(mediaState.progress)
                    is MelodiqAudioState.Playing -> _isPlaying.value = mediaState.isPlaying
                    is MelodiqAudioState.Progress -> calculateProgressValue(mediaState.progress)
                    is MelodiqAudioState.CurrentPlaying -> {
                        _currentSelectedAudio.value = _audioList.value.getOrNull(mediaState.mediaItemIndex) ?: dummyAudio
                    }

                    is MelodiqAudioState.Ready -> {
                        _duration.value = mediaState.duration
                        _uIState.value = UiState.Ready
                        calculateProgressValue(audioServiceHandler.getCurrentDuration())
                    }
                }
            }
        }

        viewModelScope.launch {
            val currentSong = playerUseCases.getCurrentSongInfoUseCase()
            currentSong?.let {
                /*currentSelectedAudio = it*/
            }
        }

        _sortType.value = audioServiceHandler.sortType.value
        audioServiceHandler.audioList.value = _audioList.value
        Timber.e("Check Audio List Size MV1: ${audioList.value.size}")
        Timber.e("Check Audio List Size MV1 Handler: ${audioServiceHandler.audioList.value.size}")
        Timber.e("Check Audio List Size Sort Type MV1: ${sortType.value}")
    }

    fun initializeListIfNeeded() {
        viewModelScope.launch {
            val existingMediaItemCount = audioServiceHandler.getMediaItemCount()
            if (existingMediaItemCount > 0) {
                _sortType.value = audioServiceHandler.sortType.value
                audioServiceHandler.audioList.value = fetchMusicUseCase(sortType.value)
                _audioList.value = audioServiceHandler.audioList.value
                _uIState.value = UiState.MusicList(_audioList.value)
                _currentSelectedAudio.value = _audioList.value.getOrNull(audioServiceHandler.getCurrentMediaItemIndex()) ?: dummyAudio
                _duration.value = audioServiceHandler.getDuration()
                calculateProgressValue(audioServiceHandler.getCurrentDuration())
                _isPlaying.value = audioServiceHandler.isPlaying()

                return@launch
            }

            _sortType.value = audioServiceHandler.sortType.value
            audioServiceHandler.audioList.value = fetchMusicUseCase(sortType.value)
            _audioList.value = audioServiceHandler.audioList.value
            _uIState.value = UiState.MusicList(_audioList.value)
            setMediaItems()
        }
    }

    private fun setMediaItems() {
        _audioList.value.map { audio ->
            MediaItem.Builder()
                .setUri(audio.contentUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setAlbumArtist(audio.artist)
                        .setDisplayTitle(audio.songTitle)
                        .setSubtitle(audio.album)
                        .build()
                )
                .build()
        }.also {
            audioServiceHandler.setMediaItemList(it)
        }
    }

    fun loadBitmapIfNeeded(context: Context, index: Int) {
        if (_audioList.value[index].cover != null) return
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = getAlbumArt(context, _audioList.value[index].contentUri)
            val updatedList = _audioList.value.toMutableList().apply {
                this[index] = this[index].copy(cover = bitmap)
            }
            _audioList.value = updatedList
        }
    }

    fun getAlbumArt(context: Context, uri: Uri): Bitmap? {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, uri)
        val data = mmr.embeddedPicture

        return if (data != null) {
            BitmapFactory.decodeByteArray(data, 0, data.size)
        } else {
            null
        }
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
            is UiEvent.SelectedAudioChange -> {
                audioServiceHandler.onPlayerEvents(
                    MelodiqPlayerEvent.SelectAudioChange,
                    selectedAudionIndex = uiEvents.index
                )
            }
            is UiEvent.UpdateProgress -> {
                audioServiceHandler.onPlayerEvents(
                    MelodiqPlayerEvent.UpdateProgress(uiEvents.newProgress)
                )
            }
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