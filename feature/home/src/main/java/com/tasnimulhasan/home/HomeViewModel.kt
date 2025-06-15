package com.tasnimulhasan.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
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
class HomeViewModel @Inject constructor(
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
    private var initialized = false

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

    private val _uIState: MutableStateFlow<UIState> = MutableStateFlow(UIState.Initial)
    val uIState: StateFlow<UIState> = _uIState.asStateFlow()

    init {
        viewModelScope.launch {
            getSortTypeUseCase().collect {
                _sortType.value = it
                Timber.e("SortType: $it")
            }
        }

        initializeListIfNeeded()

        viewModelScope.launch {
            audioServiceHandler.audioState.collectLatest { mediaState ->
                when (mediaState) {
                    MelodiqAudioState.Initial -> _uIState.value = UIState.Initial
                    is MelodiqAudioState.Buffering -> calculateProgressValue(mediaState.progress)
                    is MelodiqAudioState.Playing -> _isPlaying.value = mediaState.isPlaying
                    is MelodiqAudioState.Progress -> calculateProgressValue(mediaState.progress)
                    is MelodiqAudioState.CurrentPlaying -> {
                        _currentSelectedAudio.value = _audioList.value.getOrNull(mediaState.mediaItemIndex) ?: dummyAudio
                    }

                    is MelodiqAudioState.Ready -> {
                        _duration.value = mediaState.duration
                        _uIState.value = UIState.Ready
                        calculateProgressValue(audioServiceHandler.getCurrentDuration())
                    }
                }
            }
        }

        viewModelScope.launch {
            val currentSong = playerUseCases.getCurrentSongInfoUseCase()
            currentSong?.let {
                /*currentSelectedAudio = it*/
                Timber.e("Check Current Song: \n${it.songTitle}")
            }
        }
    }

    fun setSortType(type: SortType) {
        viewModelScope.launch {
            setSortTypeUseCase(type)
        }
    }

    fun initializeListIfNeeded() {
        if (initialized) return

        viewModelScope.launch {
            val existingMediaItemCount = audioServiceHandler.getMediaItemCount()
            if (existingMediaItemCount > 0) {
                // Sync current state instead of resetting media items
                _audioList.value = fetchMusicUseCase.execute()
                _uIState.value = UIState.MusicList(_audioList.value)

                _currentSelectedAudio.value = _audioList.value.getOrNull(audioServiceHandler.getCurrentMediaItemIndex()) ?: dummyAudio
                _duration.value = audioServiceHandler.getDuration()
                calculateProgressValue(audioServiceHandler.getCurrentDuration())
                _isPlaying.value = audioServiceHandler.isPlaying()

                initialized = true
                return@launch
            }

            // If no media items in ExoPlayer, initialize normally
            _audioList.value = fetchMusicUseCase.execute()
            _uIState.value = UIState.MusicList(_audioList.value)
            setMediaItems()
            initialized = true
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

    private fun getAlbumArt(context: Context, uri: Uri): Bitmap? {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, uri)
        val data = mmr.embeddedPicture

        return if (data != null) {
            BitmapFactory.decodeByteArray(data, 0, data.size)
        } else {
            null
        }
    }

    fun onUiEvents(uiEvents: UIEvents) = viewModelScope.launch {
        when (uiEvents) {
            is UIEvents.Backward -> playerUseCases.backwardTrackUseCase()
            is UIEvents.Forward -> playerUseCases.forwardTrackUseCase()
            is UIEvents.PlayPause -> {
                if (_isPlaying.value) playerUseCases.pause()
                else playerUseCases.play()
            }
            is UIEvents.SeekTo -> {
                val position = ((_duration.value * uiEvents.position) / 100f).toLong()
                playerUseCases.seekTo(position)
            }
            UIEvents.SeekToNext -> playerUseCases.next()
            is UIEvents.SelectedAudioChange -> {
                audioServiceHandler.onPlayerEvents(
                    MelodiqPlayerEvent.SelectAudioChange,
                    selectedAudionIndex = uiEvents.index
                )
            }
            is UIEvents.UpdateProgress -> {
                audioServiceHandler.onPlayerEvents(
                    MelodiqPlayerEvent.UpdateProgress(uiEvents.newProgress)
                )
            }
            UIEvents.SeekToPrevious -> playerUseCases.previous()
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

    fun sortTypeToDisplayString(sortType: SortType): String {
        return when (sortType) {
            SortType.DATE_MODIFIED_ASC -> "Date Modified (ASC)"
            SortType.DATE_MODIFIED_DESC -> "Date Modified (DESC)"
            SortType.NAME_ASC -> "Name (ASC)"
            SortType.NAME_DESC -> "Name (DESC)"
            SortType.ARTIST_ASC -> "Artist (ASC)"
            SortType.ARTIST_DESC -> "Artist (DESC)"
            SortType.DURATION_ASC -> "Duration (ASC)"
            SortType.DURATION_DESC -> "Duration (DESC)"
        }
    }
}

sealed class UIEvents {
    data object PlayPause : UIEvents()
    data class SelectedAudioChange(val index: Int) : UIEvents()
    data class SeekTo(val position: Float) : UIEvents()
    data object SeekToNext : UIEvents()
    data object SeekToPrevious : UIEvents()
    data object Backward : UIEvents()
    data object Forward : UIEvents()
    data class UpdateProgress(val newProgress: Float) : UIEvents()
}

sealed class UIState {
    data class MusicList(val musics: List<MusicEntity>) : UIState()
    data object Initial : UIState()
    data object Ready : UIState()
}