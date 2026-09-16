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

    /** How far the skip-forward/skip-back buttons jump. User configurable (10/15/30s). */
    @Volatile
    var seekStepMs: Long = 10_000L

    fun currentPlaybackSpeed(): Float = exoPlayer.playbackParameters.speed

    init {
        exoPlayer.addListener(this)
        if (exoPlayer.playbackState == ExoPlayer.STATE_READY) {
            _audioState.value = MelodiqAudioState.Ready(exoPlayer.duration)
            _audioState.value = MelodiqAudioState.Progress(exoPlayer.currentPosition)
            _isPlayingState.value = exoPlayer.playWhenReady
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
        val isPlaying = exoPlayer.playWhenReady

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
            MelodiqPlayerEvent.BackwardTrack5Sec ->
                exoPlayer.seekTo((exoPlayer.currentPosition - seekStepMs).coerceAtLeast(0L))
            MelodiqPlayerEvent.ForwardTrack5Sec -> {
                // Clamp to duration so a forward skip near the end doesn't overshoot into an
                // invalid position (which ExoPlayer would treat as "track finished").
                val end = exoPlayer.duration
                val target = exoPlayer.currentPosition + seekStepMs
                exoPlayer.seekTo(if (end > 0) target.coerceAtMost(end) else target)
            }
            is MelodiqPlayerEvent.SetPlaybackSpeed ->
                exoPlayer.setPlaybackSpeed(playerEvent.speed.coerceIn(0.25f, 3.0f))
            MelodiqPlayerEvent.PlayPause -> playOrPause()
            MelodiqPlayerEvent.Play -> {
                if (!exoPlayer.playWhenReady) {
                    exoPlayer.play()
                    _isPlayingState.value = true
                    startProgressUpdate()
                }
            }
            MelodiqPlayerEvent.Pause -> {
                if (exoPlayer.playWhenReady) {
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
    fun isPlaying(): Boolean = exoPlayer.playWhenReady
    fun getMediaItemCount(): Int = exoPlayer.mediaItemCount

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> _audioState.value = MelodiqAudioState.Buffering(exoPlayer.currentPosition)
            ExoPlayer.STATE_READY -> _audioState.value = MelodiqAudioState.Ready(exoPlayer.duration)
            else -> Unit
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _currentIndex.value = exoPlayer.currentMediaItemIndex
        if (isPlaying) {
            CoroutineScope(Dispatchers.Main).launch { startProgressUpdate() }
        } else {
            job?.cancel()
        }
    }

    /**
     * The play/pause icon is driven from playWhenReady, NOT isPlaying.
     *
     * isPlaying momentarily goes false whenever the engine stalls - most visibly while
     * seeking, when it re-buffers at the new position. Binding the icon to it made the
     * button flip to "play" mid-drag even though the user never paused, and left the icon
     * disagreeing with the actual transport state afterwards. playWhenReady reflects
     * intent ("should this be playing?"), which is what the button is actually showing.
     */
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        _isPlayingState.value = playWhenReady
        if (playWhenReady) {
            CoroutineScope(Dispatchers.Main).launch { startProgressUpdate() }
        } else {
            job?.cancel()
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

    /**
     * Inserts [song] directly after the currently playing item, so it plays next without
     * disturbing the rest of the queue or what's currently playing.
     */
    fun playNext(song: MusicEntity) {
        val insertAt = (exoPlayer.currentMediaItemIndex + 1).coerceIn(0, exoPlayer.mediaItemCount)
        exoPlayer.addMediaItem(insertAt, buildMediaItems(listOf(song)).first())
        audioList.value = audioList.value.toMutableList().apply { add(insertAt, song) }
        if (exoPlayer.mediaItemCount == 1) exoPlayer.prepare()
    }

    /** Appends [song] to the end of the queue. */
    fun playLater(song: MusicEntity) {
        exoPlayer.addMediaItem(buildMediaItems(listOf(song)).first())
        audioList.value = audioList.value + song
        if (exoPlayer.mediaItemCount == 1) exoPlayer.prepare()
    }

    /** Appends several songs to the end of the queue in order. */
    fun addToQueue(songs: List<MusicEntity>) {
        if (songs.isEmpty()) return
        exoPlayer.addMediaItems(buildMediaItems(songs))
        audioList.value = audioList.value + songs
        if (exoPlayer.mediaItemCount == songs.size) exoPlayer.prepare()
    }

    /**
     * Removes the queue entry at [index]. Removing the item that's currently playing makes
     * ExoPlayer advance to the next one on its own, which is the behaviour users expect.
     */
    fun removeFromQueue(index: Int) {
        if (index !in 0 until exoPlayer.mediaItemCount) return
        exoPlayer.removeMediaItem(index)
        audioList.value = audioList.value.toMutableList().apply { removeAt(index) }
        _currentIndex.value = exoPlayer.currentMediaItemIndex
    }

    /** Moves a queue entry, e.g. for drag-to-reorder in the queue screen. */
    fun moveQueueItem(from: Int, to: Int) {
        val count = exoPlayer.mediaItemCount
        if (from !in 0 until count || to !in 0 until count || from == to) return
        exoPlayer.moveMediaItem(from, to)
        audioList.value = audioList.value.toMutableList().apply { add(to, removeAt(from)) }
        _currentIndex.value = exoPlayer.currentMediaItemIndex
    }

    fun clearQueue() {
        exoPlayer.clearMediaItems()
        audioList.value = emptyList()
        _currentIndex.value = -1
    }

    private fun playOrPause() {
        // Toggle against playWhenReady (intent), not isPlaying (engine state) - otherwise
        // tapping during a seek/buffer stall reads as "currently paused" and no-ops.
        if (exoPlayer.playWhenReady) {
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
    data class SetPlaybackSpeed(val speed: Float) : MelodiqPlayerEvent()
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