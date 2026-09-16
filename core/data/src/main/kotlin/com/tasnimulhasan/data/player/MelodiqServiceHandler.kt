package com.tasnimulhasan.data.player

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

    // Dedicated state holders.
    //
    // These used to be multiplexed through `audioState` alone, which is a StateFlow and
    // therefore CONFLATED: it only guarantees the collector observes the latest value. Any
    // time two different kinds of state were published back-to-back (e.g. CurrentPlaying
    // immediately followed by Playing, or CurrentPlaying followed 500ms later by a stream
    // of Progress ticks), the earlier one was silently dropped before any collector ran.
    // That's why the "currently selected song" went stale or never appeared at all.
    //
    // Distinct concerns now get distinct StateFlows, so an update to one can never erase an
    // update to another. audioState is kept for progress/buffering/ready signalling only.
    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState: StateFlow<Boolean> = _isPlayingState.asStateFlow()

    val audioList = MutableStateFlow<List<MusicEntity>>(emptyList())
    val sortType = MutableStateFlow(SortType.DATE_MODIFIED_DESC)

    private var job: Job? = null

    init {
        exoPlayer.addListener(this)
        if (exoPlayer.playbackState == ExoPlayer.STATE_READY) {
            _audioState.value = MelodiqAudioState.Ready(exoPlayer.duration)
            _audioState.value = MelodiqAudioState.Progress(exoPlayer.currentPosition)
            _isPlayingState.value = exoPlayer.isPlaying
            _currentIndex.value = exoPlayer.currentMediaItemIndex
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
        val mediaItems = buildMediaItems(audioList)
        setMediaItemList(mediaItems)
    }

    /** Replaces the queue with a specific curated list (e.g. a playlist) and starts playing
     * it immediately at [startIndex] - distinct from [updateMediaItems], which is used for
     * loading/refreshing the full library and preserves whatever was already playing. */
    fun playCuratedQueue(audioList: List<MusicEntity>, sortType: SortType, startIndex: Int) {
        this.sortType.value = sortType
        this.audioList.value = audioList.toList()
        val mediaItems = buildMediaItems(audioList)
        val clampedIndex = startIndex.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0))
        exoPlayer.setMediaItems(mediaItems, clampedIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.play()
        _currentIndex.value = clampedIndex
        _isPlayingState.value = true
        startProgressUpdate()
    }

    fun updateMediaItemsWithCurrentTrack(
        audioList: List<MusicEntity>,
        sortType: SortType,
        restoreSongId: Long? = null,
        restorePositionMs: Long = 0L,
    ) {
        this.sortType.value = sortType
        this.audioList.value = audioList.toList()

        val mediaItems = buildMediaItems(audioList)

        val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri
        val currentPosition = exoPlayer.currentPosition
        val isPlaying = exoPlayer.isPlaying

        val newIndex: Int
        val seekPositionMs: Long
        when {
            currentUri != null -> {
                // Already have something loaded (navigating between screens) - keep it
                // exactly where it is. Unchanged from before.
                newIndex = mediaItems.indexOfFirst { it.localConfiguration?.uri == currentUri }
                    .takeIf { it >= 0 } ?: 0
                seekPositionMs = currentPosition
            }
            restoreSongId != null -> {
                // True cold start (fresh ExoPlayer, nothing loaded this process) - resume
                // from the last persisted track/position instead of defaulting to 0/0.
                val restoreIndex = audioList.indexOfFirst { it.songId == restoreSongId }
                newIndex = restoreIndex.takeIf { it >= 0 } ?: 0
                seekPositionMs = if (restoreIndex >= 0) restorePositionMs else 0L
            }
            else -> {
                newIndex = 0
                seekPositionMs = 0L
            }
        }

        exoPlayer.setMediaItems(mediaItems, newIndex, seekPositionMs)
        exoPlayer.prepare()
        if (isPlaying) {
            exoPlayer.playWhenReady = true
            startProgressUpdate()
        }
        _currentIndex.value = newIndex
    }

    fun getCurrentDuration(): Long = exoPlayer.currentPosition

    fun onPlayerEvents(
        playerEvent: MelodiqPlayerEvent,
        selectedAudionIndex: Int = -1,
        seekPosition: Long = 0
    ) {
        when (playerEvent) {
            MelodiqPlayerEvent.BackwardTrack5Sec -> exoPlayer.seekTo(exoPlayer.currentPosition - 5_000)
            MelodiqPlayerEvent.ForwardTrack5Sec -> exoPlayer.seekTo(exoPlayer.currentPosition + 5_000)
            MelodiqPlayerEvent.PlayPause -> playOrPause()
            MelodiqPlayerEvent.Play -> {
                if (!exoPlayer.isPlaying) {
                    exoPlayer.play()
                    _isPlayingState.value = true
                    startProgressUpdate()
                }
            }
            MelodiqPlayerEvent.Pause -> {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    stopProgressUpdate()
                }
            }
            MelodiqPlayerEvent.SeekTo -> exoPlayer.seekTo(seekPosition)
            MelodiqPlayerEvent.SkipNext -> exoPlayer.seekToNextMediaItem()
            MelodiqPlayerEvent.SkipPrevious -> exoPlayer.seekToPreviousMediaItem()
            MelodiqPlayerEvent.SelectAudioChange -> selectTrack(selectedAudionIndex)
            MelodiqPlayerEvent.Stop -> stopProgressUpdate()
            is MelodiqPlayerEvent.UpdateProgress -> {
                exoPlayer.seekTo((exoPlayer.duration * playerEvent.newProgress).toLong())
            }
            MelodiqPlayerEvent.RepeatTrackOne -> exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            MelodiqPlayerEvent.RepeatTrackALl -> exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
            MelodiqPlayerEvent.RepeatTrackOff -> exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
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
        _isPlayingState.value = isPlaying
        _currentIndex.value = exoPlayer.currentMediaItemIndex
        if (isPlaying) {
            CoroutineScope(Dispatchers.Main).launch { startProgressUpdate() }
        } else {
            stopProgressUpdate()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // Fires on auto-advance to the next track too - this is what keeps every screen's
        // "now playing" highlight correct without the user touching anything.
        _currentIndex.value = exoPlayer.currentMediaItemIndex
    }

    /**
     * Jumps to [index] within the currently loaded queue.
     *
     * Before seeking, this verifies the player's queue actually matches the list this
     * handler believes is loaded. If ExoPlayer's timeline is empty or a different size
     * (which is what "only the first song ever plays" looks like from the outside - every
     * index >= 1 is out of range, so the seek is rejected and playback stays on item 0),
     * the queue is rebuilt from [audioList] first and the seek is then applied to a
     * correctly populated timeline.
     */
    private fun selectTrack(index: Int) {
        val expected = audioList.value
        if (index < 0 || index >= expected.size) return

        if (exoPlayer.mediaItemCount != expected.size) {
            exoPlayer.setMediaItems(buildMediaItems(expected), index, 0L)
            exoPlayer.prepare()
        } else if (exoPlayer.currentMediaItemIndex != index) {
            exoPlayer.seekTo(index, 0L)
        }

        exoPlayer.playWhenReady = true
        exoPlayer.play()
        _currentIndex.value = index
        _isPlayingState.value = true
        startProgressUpdate()
    }

    private fun buildMediaItems(songs: List<MusicEntity>): List<MediaItem> = songs.map { audio ->
        MediaItem.Builder()
            .setMediaId(audio.songId.toString())
            .setUri(audio.contentUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(audio.songTitle)
                    .setArtist(audio.artist)
                    .setAlbumTitle(audio.album)
                    .setAlbumArtist(audio.artist)
                    .setDisplayTitle(audio.songTitle)
                    .setSubtitle(audio.album)
                    .build()
            )
            .build()
    }

    private fun playOrPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            stopProgressUpdate()
        } else {
            exoPlayer.play()
            _isPlayingState.value = true
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
        _isPlayingState.value = false
    }
}

sealed class MelodiqPlayerEvent {
    data object PlayPause : MelodiqPlayerEvent()
    data object Play : MelodiqPlayerEvent()
    data object Pause : MelodiqPlayerEvent()
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