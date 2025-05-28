package com.tasnimulhasan.featureplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.tasnimulhasan.common.service.MelodiqAudioState
import com.tasnimulhasan.common.service.MelodiqServiceHandler
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.music.FetchMusicUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val exoPlayer: ExoPlayer,
    context: Context,
) : BaseViewModel() {

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _volume = MutableStateFlow(0)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private var isAdjustingFromSlider = false

    private val dummyAudio = MusicEntity(
        contentUri = "".toUri(), songId = 0L, cover = null, songTitle = "", artist = "", duration = "", albumId = 0L, album = ""
    )

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _audioList = MutableStateFlow(listOf<MusicEntity>())
    val audioList: StateFlow<List<MusicEntity>> = _audioList.asStateFlow()

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
        _volume.value = getCurrentVolumePercent()
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
                        val newIndex = mediaState.mediaItemIndex
                        _currentSelectedAudio.value = _audioList.value[newIndex]
                    }
                    is MelodiqAudioState.Ready -> {
                        _duration.value = mediaState.duration
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
            _audioList.value = fetchMusicUseCase.execute()
            _uIState.value = UIState.MusicList(_audioList.value)
        }
        initialized = true
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
                val position = ((_duration.value * uiEvents.position) / 100f).toLong()
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
        _progress.value = if (currentProgress > 0 && _duration.value > 0) ((currentProgress.toFloat() / _duration.value.toFloat()) * 100f) else 0f

        _progressString.value = if (_showElapsedTime.value) formatDuration(currentProgress) //Minutes Seconds Value
        else formatDuration(if (_duration.value > currentProgress) _duration.value - currentProgress else 0L)

        _progressStringMinutes.value = if (_showElapsedTime.value) formatDurationMinutes(currentProgress) //Minutes Value
        else formatDurationMinutes(if (_duration.value > currentProgress) _duration.value - currentProgress else 0L)

        _progressStringSeconds.value = if (_showElapsedTime.value) formatDurationSeconds(currentProgress)//Seconds Value
        else formatDurationSeconds(if (_duration.value > currentProgress) _duration.value - currentProgress else 0L)
    }

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

    @androidx.annotation.OptIn(UnstableApi::class)
    fun setVolumeWithBoost(volumePercent: Int, fromSlider: Boolean = false) {
        isAdjustingFromSlider = fromSlider
        val clampedVolume = volumePercent.coerceIn(0, 200)
        _volume.value = clampedVolume

        // Control device volume only in 0–100 range
        if (clampedVolume <= 100) {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val newVolume = (clampedVolume * maxVolume / 100f).toInt()
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                newVolume,
                0
            )
            exoPlayer.volume = clampedVolume / 100f
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        } else {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                maxVolume,
                0
            )
            exoPlayer.volume = 1.0f
            val boostLevel = ((clampedVolume - 100) / 100f * 1000).toInt()
            loudnessEnhancer?.release()
            loudnessEnhancer = try {
                LoudnessEnhancer(exoPlayer.audioSessionId).apply {
                    setTargetGain(boostLevel)
                    enabled = true
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize LoudnessEnhancer")
                null
            }
        }
        // Reset flag after adjustment
        if (fromSlider) {
            viewModelScope.launch {
                delay(200) // Debounce to allow system volume to settle
                isAdjustingFromSlider = false
            }
        }
    }

    fun getCurrentVolumePercent(): Int {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return (currentVolume.toFloat() / maxVolume.toFloat() * 100).toInt()
    }

    fun isAdjustingFromSlider(): Boolean = isAdjustingFromSlider

    /*override fun onCleared() {
        super.onCleared()
        loudnessEnhancer?.release()
    }*/
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