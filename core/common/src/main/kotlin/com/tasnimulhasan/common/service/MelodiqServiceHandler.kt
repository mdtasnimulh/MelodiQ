package com.tasnimulhasan.common.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
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

    val audioList = MutableStateFlow<List<MusicEntity>>(emptyList())
    val sortType = MutableStateFlow(SortType.DATE_MODIFIED_DESC)

    private var job: Job? = null

    init {
        exoPlayer.addListener(this)
        if (exoPlayer.playbackState == ExoPlayer.STATE_READY) {
            _audioState.value = MelodiqAudioState.Ready(exoPlayer.duration)
            _audioState.value = MelodiqAudioState.Progress(exoPlayer.currentPosition)
            _audioState.value = MelodiqAudioState.Playing(exoPlayer.isPlaying)
            _audioState.value = MelodiqAudioState.CurrentPlaying(exoPlayer.currentMediaItemIndex)
        }
    }

    fun setMediaItemList(mediaItems: List<MediaItem>, resetPosition: Boolean = false) {
        exoPlayer.setMediaItems(mediaItems, resetPosition)
        exoPlayer.prepare()
        if (!resetPosition && mediaItems.isNotEmpty()) {
            val currentIndex = exoPlayer.currentMediaItemIndex
            if (currentIndex in mediaItems.indices) {
                exoPlayer.seekToDefaultPosition(currentIndex)
            }
        }
    }

    fun updateMediaItems(audioList: List<MusicEntity>, sortType: SortType) {
        this.sortType.value = sortType
        this.audioList.value = audioList.toList()
        val mediaItems = audioList.map { audio ->
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
        }
        setMediaItemList(mediaItems)
    }

    fun updateMediaItemsWithCurrentTrack(audioList: List<MusicEntity>, sortType: SortType) {
        this.sortType.value = sortType
        this.audioList.value = audioList.toList()
        val mediaItems = audioList.map { audio ->
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
        }

        // Preserve current playback state
        val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri
        val currentPosition = exoPlayer.currentPosition
        val isPlaying = exoPlayer.isPlaying

        // Find the new index of the current track
        val newIndex = if (currentUri != null) {
            mediaItems.indexOfFirst { it.localConfiguration?.uri == currentUri }
                .takeIf { it >= 0 } ?: exoPlayer.currentMediaItemIndex
        } else {
            exoPlayer.currentMediaItemIndex
        }

        // Set media items with the new index and position
        exoPlayer.setMediaItems(mediaItems, newIndex, if (isPlaying) currentPosition else 0L)
        exoPlayer.prepare()
        if (isPlaying) {
            exoPlayer.playWhenReady = true
            startProgressUpdate()
        }
        _audioState.value = MelodiqAudioState.CurrentPlaying(newIndex)
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
            MelodiqPlayerEvent.BackwardTrack5Sec -> exoPlayer.seekTo(exoPlayer.currentPosition - 5_000)
            MelodiqPlayerEvent.ForwardTrack5Sec -> exoPlayer.seekTo(exoPlayer.currentPosition + 5_000)
            MelodiqPlayerEvent.PlayPause -> playOrPause()
            MelodiqPlayerEvent.SeekTo -> exoPlayer.seekTo(seekPosition)
            MelodiqPlayerEvent.SkipNext -> exoPlayer.seekToNextMediaItem()
            MelodiqPlayerEvent.SkipPrevious -> exoPlayer.seekToPreviousMediaItem()
            MelodiqPlayerEvent.SelectAudioChange -> {
                if (exoPlayer.currentMediaItemIndex != selectedAudionIndex) {
                    exoPlayer.seekToDefaultPosition(selectedAudionIndex)
                    _audioState.value = MelodiqAudioState.Playing(isPlaying = true)
                    exoPlayer.playWhenReady = true
                    startProgressUpdate()
                } else if (!exoPlayer.isPlaying) {
                    exoPlayer.playWhenReady = true
                    _audioState.value = MelodiqAudioState.Playing(isPlaying = true)
                    startProgressUpdate()
                }
            }
            MelodiqPlayerEvent.Stop -> stopProgressUpdate()
            is MelodiqPlayerEvent.UpdateProgress -> {
                exoPlayer.seekTo(
                    (exoPlayer.duration * playerEvent.newProgress).toLong()
                )
            }
            MelodiqPlayerEvent.RepeatTrackOne -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            }
            MelodiqPlayerEvent.RepeatTrackALl -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
            }
            MelodiqPlayerEvent.RepeatTrackOff -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            }
        }
    }

    fun getCurrentMediaItemIndex(): Int = exoPlayer.currentMediaItemIndex

    fun getDuration(): Long = exoPlayer.duration

    fun isPlaying(): Boolean = exoPlayer.isPlaying

    fun getMediaItemCount(): Int = exoPlayer.mediaItemCount

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
    data object BackwardTrack5Sec : MelodiqPlayerEvent()
    data object SkipNext : MelodiqPlayerEvent()
    data object SkipPrevious : MelodiqPlayerEvent()
    data object ForwardTrack5Sec : MelodiqPlayerEvent()
    data object SeekTo : MelodiqPlayerEvent()
    data object Stop : MelodiqPlayerEvent()
    data class UpdateProgress(val newProgress: Float) : MelodiqPlayerEvent()
    data object RepeatTrackOne : MelodiqPlayerEvent()
    data object RepeatTrackALl : MelodiqPlayerEvent()
    data object RepeatTrackOff : MelodiqPlayerEvent()
}

sealed class MelodiqAudioState {
    data object Initial : MelodiqAudioState()
    data class Ready(val duration: Long) : MelodiqAudioState()
    data class Progress(val progress: Long) : MelodiqAudioState()
    data class Buffering(val progress: Long) : MelodiqAudioState()
    data class Playing(val isPlaying: Boolean) : MelodiqAudioState()
    data class CurrentPlaying(val mediaItemIndex: Int) : MelodiqAudioState()
}