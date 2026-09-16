package com.tasnimulhasan.featureplayer

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.datastore.GetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.player.PlaybackState
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerUseCases: PlayerUseCases,
    private val getSortTypeUseCase: GetSortTypeUseCase,
    private val exoPlayer: ExoPlayer, // volume-boost only
    private val sleepTimerController: SleepTimerController,
    savedStateHandle: SavedStateHandle,
    context: Context,
) : BaseViewModel() {

    // The song this screen was navigated to for. Read once from the nav arg via
    // SavedStateHandle (Navigation Compose populates this automatically for a type-safe
    // route) - NOT from any Compose-observed ViewModel state, which is what made the old
    // "is this already the current song" check in PlayerScreen racy: currentSelectedAudio
    // starts as a dummy and only resolves asynchronously, so a Compose LaunchedEffect
    // comparing against it could fire a real track-change for what was actually already
    // the current, playing track.
    private val targetSongId: Long = savedStateHandle.get<String>("musicId")?.toLongOrNull() ?: -1L

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

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var loudnessEnhancerSessionId: Int? = null
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _volume = MutableStateFlow(0)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _volumeGain = MutableStateFlow(0f)
    val volumeGain: StateFlow<Float> = _volumeGain.asStateFlow()

    private var isAdjustingFromSlider = false

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // Pass-throughs onto the repository's single shared StateFlow - see MainViewModel for
    // why this must not be independently fetched/derived here.
    val audioList: StateFlow<List<MusicEntity>> = playerUseCases.observeAudioList()

    val currentSelectedAudio: StateFlow<MusicEntity> = playerUseCases.observeCurrentSelectedAudio()
        .map { it ?: dummyAudio }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), dummyAudio)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _progressString = MutableStateFlow("00:00")
    val progressString = _progressString.asStateFlow()

    private val _showElapsedTime = MutableStateFlow(true)

    private val _repeatModeOne = MutableStateFlow(false)
    val repeatModeOne = _repeatModeOne.asStateFlow()

    private val _repeatModeAll = MutableStateFlow(false)
    val repeatModeAll = _repeatModeAll.asStateFlow()

    private val _repeatModeOff = MutableStateFlow(true)
    val repeatModeOff = _repeatModeOff.asStateFlow()

    private var isSeekingFromSlider = false

    val sleepTimerActive: StateFlow<Boolean> = sleepTimerController.isRunning
    val sleepTimerRemainingMillis: StateFlow<Long> = sleepTimerController.remainingMillis

    private val _uIState: MutableStateFlow<UIState> = MutableStateFlow(UIState.Initial)
    val uIState: StateFlow<UIState> = _uIState.asStateFlow()

    init {
        viewModelScope.launch {
            getSortTypeUseCase().collectLatest { persistedSortType ->
                _sortType.value = persistedSortType
            }
        }

        // NOTE: Home/Songs/AlbumDetails already call selectAudioChange themselves before
        // navigating here, only when the tapped song is actually different from what's
        // currently loaded. Re-deriving and re-applying "the right song" again here raced
        // against that first call and could misfire - e.g. opening the player for the
        // already-selected-but-paused song would sometimes force it back to playing,
        // because this block ran before the original selection had taken effect and
        // concluded (incorrectly) that a re-selection was needed. The player screen should
        // only ever reflect state, never decide on its own to change what's playing.

        viewModelScope.launch {
            playerUseCases.observeAudioState().collectLatest { mediaState ->
                when (mediaState) {
                    PlaybackState.Idle -> _uIState.value = UIState.Initial
                    is PlaybackState.Buffering -> calculateProgressValue(mediaState.position)
                    is PlaybackState.Playing -> _isPlaying.value = mediaState.isPlaying
                    is PlaybackState.Progress -> calculateProgressValue(mediaState.position)
                    is PlaybackState.TrackChanged -> Unit
                    is PlaybackState.Ready -> {
                        _duration.value = mediaState.duration
                        _uIState.value = UIState.Ready
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
        _isPlaying.value = snapshot.isPlaying
    }

    fun toggleTimeDisplay() {
        _showElapsedTime.value = !_showElapsedTime.value
        viewModelScope.launch {
            calculateProgressValue(playerUseCases.getPlaybackSnapshot().position)
        }
    }

    fun onUiEvents(uiEvents: UIEvents) = viewModelScope.launch {
        when (uiEvents) {
            UIEvents.Backward -> playerUseCases.backwardTrackUseCase()
            UIEvents.Forward -> playerUseCases.forwardTrackUseCase()
            is UIEvents.PlayPause -> {
                if (_isPlaying.value) playerUseCases.pause()
                else playerUseCases.play()
            }
            is UIEvents.SeekTo -> {
                if (isSeekingFromSlider) return@launch
                isSeekingFromSlider = true
                val position = ((_duration.value * uiEvents.position) / 100f).toLong()
                playerUseCases.seekTo(position)
                delay(50.milliseconds)
                isSeekingFromSlider = false
            }
            UIEvents.SeekToNext -> playerUseCases.next()
            is UIEvents.SelectedAudioChange -> playerUseCases.selectAudioChange(uiEvents.index)
            is UIEvents.UpdateProgress -> playerUseCases.updateProgress(uiEvents.newProgress)
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
        }
    }

    private fun calculateProgressValue(currentProgress: Long) {
        _progress.value =
            if (currentProgress > 0 && _duration.value > 0) ((currentProgress.toFloat() / _duration.value.toFloat()) * 100f)
            else 0f
        _progressString.value = if (_showElapsedTime.value) formatDuration(currentProgress)
        else formatDuration(if (_duration.value > currentProgress) _duration.value - currentProgress else 0L)
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

    @androidx.annotation.OptIn(UnstableApi::class)
    fun setVolumeWithBoost(volumePercent: Int, fromSlider: Boolean = false) {
        isAdjustingFromSlider = fromSlider
        val clampedVolume = volumePercent.coerceIn(0, 200)
        _volume.value = clampedVolume
        _volumeGain.value = clampedVolume / 200f

        if (clampedVolume <= 100) {
            // Only touch the system stream when the target actually changes - calling
            // setStreamVolume on every slider tick (many times a second while dragging)
            // causes audible pops and UI jank for no benefit once we're already there.
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val newVolume = (clampedVolume * maxVolume / 100f).toInt()
            if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != newVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
            }
            exoPlayer.volume = clampedVolume / 100f
            // Leave boost mode - release the enhancer once, not on every tick below 100.
            if (loudnessEnhancer != null) {
                loudnessEnhancer?.release()
                loudnessEnhancer = null
                loudnessEnhancerSessionId = null
            }
        } else {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != maxVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
            }
            exoPlayer.volume = 1.0f
            val boostLevel = ((clampedVolume - 100) / 100f * 1000).toInt()
            // Reuse the existing enhancer and just move its target gain - creating a fresh
            // LoudnessEnhancer (native AudioEffect JNI calls) on every drag tick is what was
            // causing the boost slider to feel laggy/stuttery.
            val sessionId = exoPlayer.audioSessionId
            val enhancer = loudnessEnhancer
            if (enhancer != null && loudnessEnhancerSessionId == sessionId) {
                try {
                    enhancer.setTargetGain(boostLevel)
                } catch (_: Exception) {
                    // Fall through and recreate below if the existing instance is unusable.
                    recreateLoudnessEnhancer(sessionId, boostLevel)
                }
            } else {
                recreateLoudnessEnhancer(sessionId, boostLevel)
            }
        }

        if (fromSlider) {
            viewModelScope.launch {
                delay(200.milliseconds)
                isAdjustingFromSlider = false
            }
        }
    }

    private fun recreateLoudnessEnhancer(sessionId: Int, boostLevel: Int) {
        loudnessEnhancer?.release()
        loudnessEnhancer = try {
            LoudnessEnhancer(sessionId).apply {
                setTargetGain(boostLevel)
                enabled = true
            }.also { loudnessEnhancerSessionId = sessionId }
        } catch (_: Exception) {
            loudnessEnhancerSessionId = null
            null
        }
    }

    fun getCurrentVolumePercent(): Int {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return (currentVolume.toFloat() / maxVolume.toFloat() * 100).toInt()
    }

    fun isAdjustingFromSlider(): Boolean = isAdjustingFromSlider

    fun setVolumeGain(gain: Float) {
        _volumeGain.value = gain
        setVolumeWithBoost((gain * 200).toInt(), fromSlider = true)
    }

    fun startSleepTimer(totalDurationMillis: Long) {
        sleepTimerController.start(totalDurationMillis) {
            onUiEvents(UIEvents.PlayPause)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    fun startEndOfSongSleepTimer() {
        sleepTimerController.startEndOfSong {
            onUiEvents(UIEvents.PlayPause)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    fun cancelSleepTimer() {
        sleepTimerController.cancel()
    }

    override fun onCleared() {
        loudnessEnhancer?.release()
        super.onCleared()
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