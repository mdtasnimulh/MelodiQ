package com.tasnimulhasan.common.service

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

class MelodiqServiceHandler @Inject constructor(
    private val exoPlayer: ExoPlayer
) : Player.Listener {

    private val _audioState: MutableStateFlow<MelodiqAudioState> = MutableStateFlow(MelodiqAudioState.Initial)
    val audioState: StateFlow<MelodiqAudioState> = _audioState.asStateFlow()

    private var job: Job? = null

    init {
        exoPlayer.addListener(this)
    }

    fun addMediaItem(mediaItem: MediaItem) {
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    fun setMediaItemList(mediaItems: List<MediaItem>) {
        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()
    }

    fun getCurrentDuration(): Long {
        return exoPlayer.currentPosition
    }

    fun onPlayerEvents(
        playerEvent: MelodiqPlayerEvent,
        selectedAudionIndex: Int = -1,
        seekPosition: Long = 0
    ) {
        when (playerEvent) {
            MelodiqPlayerEvent.Backward -> exoPlayer.seekTo(exoPlayer.currentPosition - 5_000)
            MelodiqPlayerEvent.Forward -> exoPlayer.seekTo(exoPlayer.currentPosition + 5_000)
            MelodiqPlayerEvent.PlayPause -> playOrPause()
            MelodiqPlayerEvent.SeekTo -> exoPlayer.seekTo(seekPosition)
            MelodiqPlayerEvent.SeekToNext -> exoPlayer.seekToNextMediaItem()
            MelodiqPlayerEvent.SeekToPrevious -> exoPlayer.seekToPreviousMediaItem()
            MelodiqPlayerEvent.SelectAudioChange -> {
                exoPlayer.seekToDefaultPosition(selectedAudionIndex)
                _audioState.value = MelodiqAudioState.Playing(isPlaying = true)
                exoPlayer.playWhenReady = true
                startProgressUpdate()
                /*when (selectedAudionIndex) {
                    exoPlayer.currentMediaItemIndex -> {
                        playOrPause()
                    }
                    else -> {
                        exoPlayer.seekToDefaultPosition(selectedAudionIndex)
                        _audioState.value = MelodiqAudioState.Playing(isPlaying = true)
                        exoPlayer.playWhenReady = true
                        startProgressUpdate()
                    }
                }*/
            }
            MelodiqPlayerEvent.Stop -> stopProgressUpdate()
            is MelodiqPlayerEvent.UpdateProgress -> {
                exoPlayer.seekTo(
                    (exoPlayer.duration * playerEvent.newProgress).toLong()
                )
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> _audioState.value = MelodiqAudioState.Buffering(exoPlayer.currentPosition)
            ExoPlayer.STATE_READY -> _audioState.value = MelodiqAudioState.Ready(exoPlayer.duration)
            else -> Unit
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _audioState.value = MelodiqAudioState.Playing(isPlaying = isPlaying)
        _audioState.value = MelodiqAudioState.CurrentPlaying(exoPlayer.currentMediaItemIndex)
        if (isPlaying) {
            CoroutineScope(Dispatchers.Main).launch {
                startProgressUpdate()
            }
        } else {
            stopProgressUpdate()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        _audioState.value = MelodiqAudioState.CurrentPlaying(exoPlayer.currentMediaItemIndex)
    }

    private fun playOrPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            stopProgressUpdate()
        }else {
            exoPlayer.play()
            _audioState.value = MelodiqAudioState.Playing(
                isPlaying = true
            )
            startProgressUpdate()
        }
    }

    private fun startProgressUpdate() = job.run {
        /*while (true) {
            delay(500)
            _audioState.value = MelodiqAudioState.Progress(exoPlayer.currentPosition)
        }*/
        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                _audioState.value = MelodiqAudioState.Progress(exoPlayer.currentPosition)
                delay(500)
            }
        }
    }

    private fun stopProgressUpdate() {
        job?.cancel()
        _audioState.value = MelodiqAudioState.Playing(isPlaying = false)
    }

}

sealed class MelodiqPlayerEvent {
    data object PlayPause : MelodiqPlayerEvent()
    data object SelectAudioChange : MelodiqPlayerEvent()
    data object Backward : MelodiqPlayerEvent()
    data object SeekToNext : MelodiqPlayerEvent()
    data object SeekToPrevious : MelodiqPlayerEvent()
    data object Forward : MelodiqPlayerEvent()
    data object SeekTo : MelodiqPlayerEvent()
    data object Stop : MelodiqPlayerEvent()
    data class UpdateProgress(val newProgress: Float) : MelodiqPlayerEvent()
}

sealed class MelodiqAudioState {
    data object Initial : MelodiqAudioState()
    data class Ready(val duration: Long) : MelodiqAudioState()
    data class Progress(val progress: Long) : MelodiqAudioState()
    data class Buffering(val progress: Long) : MelodiqAudioState()
    data class Playing(val isPlaying: Boolean) : MelodiqAudioState()
    data class CurrentPlaying(val mediaItemIndex: Int) : MelodiqAudioState()
}