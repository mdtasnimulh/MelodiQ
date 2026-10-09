package com.tasnimulhasan.data.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import android.content.Context
import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.VisualizerStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.pow
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

class MelodiqServiceHandler @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val preferencesDataStoreRepository: PreferencesDataStoreRepository,
    @ApplicationContext private val context: Context,
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

    // --- Volume normalization (ReplayGain) + crossfade -----------------------------------
    // exoPlayer.volume is a single float, and both features want to drive it independently
    // (a per-track loudness correction vs. a fade envelope around track transitions), so the
    // two are tracked as separate multipliers and multiplied together on every change. This
    // is the only thing either feature touches - no changes to the queue, timeline, media
    // session, or notification.
    @Volatile
    private var replayGainEnabled: Boolean = false

    @Volatile
    private var crossfadeEnabled: Boolean = false

    @Volatile
    private var crossfadeDurationMs: Long = 4_000L

    private var replayGainMultiplier: Float = 1f
    private var crossfadeEnvelope: Float = 1f
    private var crossfadeJob: Job? = null

    // Separate from crossfadeEnvelope: this is a one-shot fade used by the sleep timer's
    // "fade out" option, not something the 500ms progress tick or track transitions ever
    // touch. Combined into applyVolume() like the other two multipliers.
    private var sleepFadeMultiplier: Float = 1f
    private var sleepFadeJob: Job? = null

    // --- Visualizer ------------------------------------------------------------------------
    // OFF by default and inactive (no capture, no permission prompt) unless the user opts in
    // from Settings. Re-attach is attempted on every onIsPlayingChanged(true) rather than
    // some dedicated "session ready" callback - ExoPlayer's audioSessionId is usually stable
    // for the player's lifetime, and attach() is cheap to call repeatedly (see its own doc).
    private val visualizerController = VisualizerController(context)
    val visualizerBars: StateFlow<FloatArray> = visualizerController.bars
    val visualizerActive: StateFlow<Boolean> = visualizerController.isActive

    @Volatile
    private var visualizerStyle: VisualizerStyle = VisualizerStyle.OFF

    // Long-lived scope for the preference collectors below and for the crossfade fade job -
    // distinct from the per-call ad-hoc scopes elsewhere in this class so those are left
    // untouched.
    private val handlerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun currentPlaybackSpeed(): Float = exoPlayer.playbackParameters.speed

    init {
        exoPlayer.addListener(this)
        handlerScope.launch {
            preferencesDataStoreRepository.getReplayGainEnabled().collect { enabled ->
                replayGainEnabled = enabled
                // Toggling off must restore full volume immediately, not just stop applying
                // future gain - otherwise the last-applied reduction would linger.
                replayGainMultiplier = 1f
                applyVolume()
            }
        }
        handlerScope.launch {
            preferencesDataStoreRepository.getCrossfadeEnabled().collect { enabled ->
                crossfadeEnabled = enabled
                if (!enabled) {
                    crossfadeJob?.cancel()
                    crossfadeEnvelope = 1f
                    applyVolume()
                }
            }
        }
        handlerScope.launch {
            preferencesDataStoreRepository.getCrossfadeDurationMs().collect { durationMs ->
                crossfadeDurationMs = durationMs
            }
        }
        handlerScope.launch {
            preferencesDataStoreRepository.getVisualizerStyle().collect { style ->
                visualizerStyle = style
                if (style == VisualizerStyle.OFF) {
                    visualizerController.release()
                } else {
                    visualizerController.attach(exoPlayer.audioSessionId)
                }
            }
        }
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

    /**
     * [prebuiltItems] lets the caller build the (potentially thousands of) MediaItems on a
     * background thread so only the unavoidable ExoPlayer call runs on the main thread.
     */
    fun updateMediaItems(
        audioList: List<MusicEntity>,
        sortType: SortType,
        prebuiltItems: List<MediaItem>? = null,
    ) {
        this.sortType.value = sortType
        this.audioList.value = audioList.toList()
        val mediaItems = prebuiltItems ?: buildMediaItems(audioList)
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
        // A curated queue (playlist) is a closed set: Next/Previous should wrap within it
        // instead of stopping at the first/last track.
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
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
        prebuiltItems: List<MediaItem>? = null,
    ) {
        this.sortType.value = sortType
        this.audioList.value = audioList.toList()

        val mediaItems = prebuiltItems ?: buildMediaItems(audioList)

        val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri
        val currentPosition = exoPlayer.currentPosition
        val isPlaying = exoPlayer.playWhenReady

        val newIndex: Int
        val seekPositionMs: Long
        when {
            currentUri != null -> {
                // Already have something loaded (navigating between screens) - keep it
                // exactly where it is. Unchanged from before.
                val found = mediaItems.indexOfFirst { it.localConfiguration?.uri == currentUri }
                newIndex = found.takeIf { it >= 0 } ?: 0
                // If the playing song is no longer in the list (deleted/moved), don't carry
                // its position over to whatever song lands at index 0 - start that one fresh.
                seekPositionMs = if (found >= 0) currentPosition else 0L
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

    /** Silences everything for "close player": pause, rewind, stop the progress ticker and
     * visualizer. The queue stays loaded so tapping any song later just works. */
    fun stopForClose() {
        stopProgressUpdate()
        exoPlayer.pause()
        exoPlayer.seekTo(0L)
        _isPlayingState.value = false
        releaseVisualizer()
    }

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
            MelodiqPlayerEvent.SeekTo -> {
                resetCrossfadeEnvelope()
                exoPlayer.seekTo(seekPosition)
            }
            MelodiqPlayerEvent.SkipNext -> {
                resetCrossfadeEnvelope()
                exoPlayer.seekToNextMediaItem()
            }
            MelodiqPlayerEvent.SkipPrevious -> {
                resetCrossfadeEnvelope()
                exoPlayer.seekToPreviousMediaItem()
            }
            MelodiqPlayerEvent.SelectAudioChange -> {
                resetCrossfadeEnvelope()
                selectTrack(selectedAudionIndex)
            }
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

    private fun applyVolume() {
        exoPlayer.volume = (replayGainMultiplier * crossfadeEnvelope * sleepFadeMultiplier).coerceIn(0f, 1f)
    }

    /** Ramps volume down to silent over [durationMs], pauses, then restores the multiplier
     * to 1 so a later play() isn't silently muted. Suspends until done - the sleep timer
     * awaits this before it proceeds with whatever happens after playback stops. A duration
     * of 0 (or less) pauses immediately with no fade. */
    suspend fun fadeOutAndPause(durationMs: Long) {
        sleepFadeJob?.cancel()
        if (durationMs <= 0L) {
            sleepFadeMultiplier = 0f
            applyVolume()
            onPlayerEvents(MelodiqPlayerEvent.Pause)
            sleepFadeMultiplier = 1f
            applyVolume()
            return
        }
        val steps = (durationMs / 100L).coerceAtLeast(1)
        val start = sleepFadeMultiplier
        for (i in 1..steps) {
            sleepFadeMultiplier = start + (0f - start) * (i / steps.toFloat())
            applyVolume()
            delay(100)
        }
        sleepFadeMultiplier = 0f
        applyVolume()
        onPlayerEvents(MelodiqPlayerEvent.Pause)
        // Restore for the next time the user presses play - this fade must never linger
        // into a future playback session.
        sleepFadeMultiplier = 1f
        applyVolume()
    }

    /**
     * ID3 TXXX ("REPLAYGAIN_TRACK_GAIN") for mp3, or a Vorbis comment of the same name for
     * ogg/flac - whichever the file actually has embedded. Track gain is preferred over
     * album gain since these are individual songs, not an album being played straight
     * through. Files with no such tag simply keep the default multiplier of 1 (untouched).
     */
    override fun onMetadata(metadata: Metadata) {
        if (!replayGainEnabled) return
        var trackGainDb: Float? = null
        var albumGainDb: Float? = null
        for (i in 0 until metadata.length()) {
            when (val entry = metadata.get(i)) {
                is TextInformationFrame -> {
                    val desc = entry.description?.uppercase()
                    val value = entry.values.firstOrNull()
                    if (value != null) {
                        when (desc) {
                            "REPLAYGAIN_TRACK_GAIN" -> trackGainDb = parseGainDb(value)
                            "REPLAYGAIN_ALBUM_GAIN" -> albumGainDb = parseGainDb(value)
                        }
                    }
                }
                is VorbisComment -> {
                    when (entry.key.uppercase()) {
                        "REPLAYGAIN_TRACK_GAIN" -> trackGainDb = parseGainDb(entry.value)
                        "REPLAYGAIN_ALBUM_GAIN" -> albumGainDb = parseGainDb(entry.value)
                    }
                }
                else -> Unit
            }
        }
        val gainDb = trackGainDb ?: albumGainDb ?: return
        // Linear gain = 10^(dB/20). Clamped to <= 1: ExoPlayer's volume can attenuate but not
        // amplify past unity, so a positive tag (rare - a track quieter than the target
        // loudness) is left at full volume rather than clipped.
        replayGainMultiplier = 10f.pow(gainDb / 20f).coerceIn(0f, 1f)
        applyVolume()
    }

    private fun parseGainDb(raw: String): Float? =
        raw.trim().removeSuffix("dB").removeSuffix("DB").trim().toFloatOrNull()

    private fun fadeEnvelopeTo(target: Float, durationMs: Long) {
        crossfadeJob?.cancel()
        val safeDuration = durationMs.coerceAtLeast(100L)
        crossfadeJob = handlerScope.launch {
            val steps = (safeDuration / 100L).coerceAtLeast(1)
            val start = crossfadeEnvelope
            for (i in 1..steps) {
                if (!isActive) return@launch
                crossfadeEnvelope = start + (target - start) * (i / steps.toFloat())
                applyVolume()
                delay(100)
            }
            crossfadeEnvelope = target
            applyVolume()
        }
    }

    /** Cancels any in-progress fade and snaps straight back to full volume - used whenever
     * the user interacts manually (skip/seek/select), so a fade-out in progress can never
     * leave playback stuck quiet after a manual jump. */
    private fun resetCrossfadeEnvelope() {
        crossfadeJob?.cancel()
        crossfadeEnvelope = 1f
        applyVolume()
    }

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
            // Cheap no-op if already attached to this session (see attach()'s own doc) -
            // this is what catches the case where the style preference turned on before
            // exoPlayer.audioSessionId was actually assigned yet.
            if (visualizerStyle != VisualizerStyle.OFF && !visualizerController.isActive.value) {
                visualizerController.attach(exoPlayer.audioSessionId)
            }
        } else {
            job?.cancel()
        }
    }

    /** Called when the service is torn down - stops any live audio capture immediately
     * rather than leaving it running past the player's own lifetime. */
    fun releaseVisualizer() = visualizerController.release()

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
        // Reset per-track gain; onMetadata (below) overwrites this once the new track's tags,
        // if any, are parsed. Without this a quiet track's gain would incorrectly linger onto
        // the next track for the brief window before its own metadata arrives.
        replayGainMultiplier = 1f
        if (crossfadeEnabled && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            // Auto-advance only - a manual skip should be instant, not muted-then-faded.
            crossfadeEnvelope = 0f
            applyVolume()
            fadeEnvelopeTo(1f, crossfadeDurationMs)
        } else {
            crossfadeEnvelope = 1f
            applyVolume()
        }
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
        android.util.Log.d(
            "MelodiQ/Select",
            "selectTrack requested=$index listSize=${expected.size} " +
                "playerItems=${exoPlayer.mediaItemCount} playerIndex=${exoPlayer.currentMediaItemIndex}"
        )
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
        android.util.Log.d(
            "MelodiQ/Select",
            "selectTrack done requested=$index playerIndex=${exoPlayer.currentMediaItemIndex} " +
                "uri=${exoPlayer.currentMediaItem?.localConfiguration?.uri}"
        )
    }

    /** Pure function - safe to call from any thread. */
    fun buildMediaItems(songs: List<MusicEntity>): List<MediaItem> = songs.map { audio ->
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
                    // Lets Media3's notification / lock screen ask the BitmapLoader for cover art.
                    .setArtworkUri(audio.contentUri)
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
                maybeScheduleCrossfadeOut()
                delay(500)
            }
        }
    }

    /** Piggybacks on the existing 500ms progress tick: once the current track is within
     * [crossfadeDurationMs] of its end AND another track follows, start fading volume down so
     * it reaches (about) zero right as the gapless transition happens. [onMediaItemTransition]
     * then fades the next track back in - together these approximate a crossfade using a
     * single player/timeline, so the existing queue, media session and notification code
     * needs no changes. This is a sequential fade-out/fade-in, not true overlapping playback
     * of two decoders at once. */
    private fun maybeScheduleCrossfadeOut() {
        if (!crossfadeEnabled) return
        if (crossfadeJob?.isActive == true) return
        val duration = exoPlayer.duration
        if (duration <= 0 || duration == androidx.media3.common.C.TIME_UNSET) return
        if (!exoPlayer.hasNextMediaItem()) return
        val remaining = duration - exoPlayer.currentPosition
        if (remaining in 0..crossfadeDurationMs && crossfadeEnvelope > 0f) {
            fadeEnvelopeTo(0f, remaining.coerceAtMost(crossfadeDurationMs))
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