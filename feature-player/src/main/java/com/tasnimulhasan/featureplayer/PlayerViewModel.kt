package com.tasnimulhasan.featureplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.tasnimulhasan.common.service.MelodiqAudioState
import com.tasnimulhasan.common.service.MelodiqPlayerEvent
import com.tasnimulhasan.common.service.MelodiqServiceHandler
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.music.FetchMusicUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
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
@OptIn(SavedStateHandleSaveableApi::class)
class PlayerViewModel @Inject constructor(
    private val fetchMusicUseCase: FetchMusicUseCase,
    private val playerUseCases: PlayerUseCases,
    private val audioServiceHandler: MelodiqServiceHandler,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val dummyAudio = MusicEntity(
        contentUri = "".toUri(), songId = 0L, cover = null, songTitle = "", artist = "", duration = "", albumId = 0L, album = ""
    )

    var duration by savedStateHandle.saveable { mutableLongStateOf(0L) }
    //var progress by savedStateHandle.saveable { mutableFloatStateOf(0f) }
    //var progressString by savedStateHandle.saveable { mutableStateOf("00:00") }
    //var isPlaying by savedStateHandle.saveable { mutableStateOf(false) }
    //var currentSelectedAudio by savedStateHandle.saveable { mutableStateOf(dummyAudio) }
    var audioList by savedStateHandle.saveable { mutableStateOf(listOf<MusicEntity>()) }

    private val _currentSelectedAudio = MutableStateFlow(dummyAudio)
    val currentSelectedAudio = _currentSelectedAudio.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _progressString = MutableStateFlow("00:00")
    val progressString = _progressString.asStateFlow()

    private val _progressStringMinutes = MutableStateFlow("00")
    val progressStringMinutes = _progressStringMinutes.asStateFlow()

    private val _progressStringSeconds = MutableStateFlow("00")
    val progressStringSeconds = _progressStringSeconds.asStateFlow()

    private var initialized = false

    private val _showElapsedTime = MutableStateFlow(true) // Default to elapsed time
    val showElapsedTime = _showElapsedTime.asStateFlow()

    private val _repeatModeOne = MutableStateFlow(false)
    val repeatModeOne = _repeatModeOne.asStateFlow()

    private val _repeatModeAll = MutableStateFlow(false)
    val repeatModeAll = _repeatModeAll.asStateFlow()

    private val _repeatModeOff = MutableStateFlow(true)
    val repeatModeOff = _repeatModeOff.asStateFlow()

    fun toggleTimeDisplay() {
        _showElapsedTime.value = !_showElapsedTime.value
        // Recalculate progressString with the new mode
        calculateProgressValue(audioServiceHandler.audioState.value.let { state ->
            when (state) {
                is MelodiqAudioState.Progress -> state.progress
                is MelodiqAudioState.Buffering -> state.progress
                else -> 0L
            }
        })
    }

    private val _uIState: MutableStateFlow<UIState> = MutableStateFlow(UIState.Initial)
    val uIState: StateFlow<UIState> = _uIState.asStateFlow()

    init {
        fetchMusicList()
        observeAudioState()
        viewModelScope.launch {
            val currentSong = playerUseCases.getCurrentSongInfoUseCase()
            currentSong?.let {
                /*currentSelectedAudio.value = it*/
                Timber.e("Check Current Song: \n${it.songTitle}")
            }
        }
    }

    private fun observeAudioState() {
        viewModelScope.launch {
            playerUseCases.observeAudioState().collectLatest { mediaState ->
                when (mediaState) {
                    MelodiqAudioState.Initial -> _uIState.value = UIState.Initial
                    is MelodiqAudioState.Buffering -> calculateProgressValue(mediaState.progress)
                    is MelodiqAudioState.Playing -> _isPlaying.value = mediaState.isPlaying
                    is MelodiqAudioState.Progress -> calculateProgressValue(mediaState.progress)
                    is MelodiqAudioState.CurrentPlaying -> {
                        /*_currentSelectedAudio.value = audioList.getOrNull(mediaState.mediaItemIndex) ?: dummyAudio*/
                        val newIndex = mediaState.mediaItemIndex
                        _currentSelectedAudio.value = audioList.getOrNull(newIndex) ?: dummyAudio
                    }
                    is MelodiqAudioState.Ready -> {
                        duration = mediaState.duration
                        _uIState.value = UIState.Ready
                        calculateProgressValue(audioServiceHandler.getCurrentDuration())
                    }
                }
            }
        }
    }

    private fun fetchMusicList() {
        if (initialized) return
        viewModelScope.launch {
            audioList = fetchMusicUseCase.execute()
            _uIState.value = UIState.MusicList(audioList)
            //setMediaItems()
        }
        initialized = true
    }

    fun getSelectedMusic(id: String) : MusicEntity {
        return audioList.find { it.songId.toString() == id }!!
    }

    fun loadBitmapIfNeeded(context: Context, index: Int) {
        if (audioList[index].cover != null) return
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = getAlbumArt(context, audioList[index].contentUri)
            val updatedList = audioList.toMutableList().apply {
                this[index] = this[index].copy(cover = bitmap)
            }
            audioList = updatedList
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
            UIEvents.Backward -> playerUseCases.backwardTrackUseCase()
            UIEvents.Forward -> playerUseCases.forwardTrackUseCase()
            UIEvents.SeekToNext -> playerUseCases.next()
            UIEvents.SeekToPrevious -> playerUseCases.previous()

            UIEvents.RepeatOne -> {
                _repeatModeOff.value = false
                _repeatModeOne.value = true
                playerUseCases.repeatTrackOneUseCase()
            }
            UIEvents.RepeatAll -> {
                _repeatModeOff.value = false
                _repeatModeAll.value = true
                playerUseCases.repeatTrackAllUseCase()
            }
            UIEvents.RepeatOff -> {
                _repeatModeOne.value = false
                _repeatModeAll.value = false
                _repeatModeOff.value = true
                playerUseCases.repeatTrackOffUseCase()
            }

            is UIEvents.PlayPause -> {
                if (_isPlaying.value) playerUseCases.pause()
                else playerUseCases.play()
            }
            is UIEvents.SeekTo -> {
                val position = ((duration * uiEvents.position) / 100f).toLong()
                playerUseCases.seekTo(position)
            }
            is UIEvents.SelectedAudioChange -> {
                playerUseCases.selectAudioChange(uiEvents.index)
            }
            is UIEvents.UpdateProgress -> {
                playerUseCases.updateProgress(uiEvents.newProgress)
            }
        }
    }

    private fun calculateProgressValue(currentProgress: Long) {
        _progress.value = if (currentProgress > 0 && duration > 0) ((currentProgress.toFloat() / duration.toFloat()) * 100f) else 0f

        _progressString.value = if (_showElapsedTime.value) formatDuration(currentProgress) //Minutes Seconds Value
        else formatDuration(if (duration > currentProgress) duration - currentProgress else 0L)

        _progressStringMinutes.value = if (_showElapsedTime.value) formatDurationMinutes(currentProgress) //Minutes Value
        else formatDurationMinutes(if (duration > currentProgress) duration - currentProgress else 0L)

        _progressStringSeconds.value = if (_showElapsedTime.value) formatDurationSeconds(currentProgress)//Seconds Value
        else formatDurationSeconds(if (duration > currentProgress) duration - currentProgress else 0L)
    }

    /*private fun calculateElapsedTime(currentProgress: Long) {
        _progress.value =
            if (currentProgress > 0) ((currentProgress.toFloat() / duration.toFloat()) * 100f)
            else 0f
        _progressString.value = formatDuration(currentProgress) // later changed to upper method using condition
    }*/

    private fun formatDuration(duration: Long): String {
        val minute = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minute, seconds)
    }

    private fun formatDurationMinutes(duration: Long): String {
        val minute = TimeUnit.MILLISECONDS.toMinutes(duration)
        return String.format(Locale.getDefault(), "%02d", minute)
    }

    private fun formatDurationSeconds(duration: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
        return String.format(Locale.getDefault(), "%02d", seconds)
    }

    fun convertLongToReadableDateTime(time: Long, format: String): String {
        val df = SimpleDateFormat(format, Locale.US)
        return df.format(time)
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
    data object RepeatOne : UIEvents()
    data object RepeatAll : UIEvents()
    data object RepeatOff : UIEvents()
}

sealed class UIState {
    data class MusicList(val musics: List<MusicEntity>) : UIState()
    data object Initial : UIState()
    data object Ready : UIState()
}