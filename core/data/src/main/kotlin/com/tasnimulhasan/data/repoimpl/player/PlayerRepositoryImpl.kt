package com.tasnimulhasan.data.repoimpl.player

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tasnimulhasan.data.library.LibraryChangeNotifier
import com.tasnimulhasan.data.player.MelodiqAudioState
import com.tasnimulhasan.data.player.MelodiqPlayerEvent
import com.tasnimulhasan.data.player.MelodiqPlayerService
import com.tasnimulhasan.data.player.MelodiqServiceHandler
import com.tasnimulhasan.domain.player.PlaybackSnapshot
import com.tasnimulhasan.domain.player.PlaybackState
import com.tasnimulhasan.domain.repository.PlayerRepository
import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.domain.repository.local.LibraryRepository
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val serviceHandler: MelodiqServiceHandler,
    private val preferencesDataStoreRepository: PreferencesDataStoreRepository,
    private val libraryRepository: LibraryRepository,
    private val libraryChangeNotifier: LibraryChangeNotifier,
    @ApplicationContext private val context: Context,
) : PlayerRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    private val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // "Browsable library" - what Home/Songs/Albums render as "all songs". Written only by
    // the library-loading pipeline below, never by playCuratedQueue.
    private val _audioList = MutableStateFlow<List<MusicEntity>>(emptyList())
    override val audioList: StateFlow<List<MusicEntity>> = _audioList.asStateFlow()

    // Whatever list is ACTUALLY loaded into ExoPlayer right now - the full library most of
    // the time, but a smaller curated list (e.g. a playlist) when playCuratedQueue was used
    // last. currentSelectedAudio must always resolve against this, not _audioList, or
    // playing a playlist track would look up the wrong song using an index that's only
    // valid within that smaller queue.
    private val _activeQueue = MutableStateFlow<List<MusicEntity>>(emptyList())

    private val _currentIndex = MutableStateFlow(-1)

    // False on a fresh install / after "close player": the queue is loaded in ExoPlayer but
    // nothing counts as "the current song" (no mini player, no highlight, nothing saved as
    // last played). Becomes true once something is restored from a saved last-played track
    // or the user actually starts playing.
    private val _sessionActive = MutableStateFlow(false)

    override val currentSelectedAudio: StateFlow<MusicEntity?> =
        combine(_activeQueue, _currentIndex, _sessionActive) { list, index, active ->
            if (active) list.getOrNull(index) else null
        }
            .stateIn(repositoryScope, SharingStarted.Eagerly, null)

    override val isPlaying: StateFlow<Boolean> = serviceHandler.isPlayingState

    private var progressTickCount = 0

    // Declared before init{} - init launches coroutines that read/write this.
    private var lastSortType: SortType = SortType.DATE_MODIFIED_DESC
    private val initialQueueLoaded = CompletableDeferred<Unit>()

    init {
        // Index and play/pause now come from dedicated StateFlows rather than being
        // filtered out of the single multiplexed `audioState`. Sharing one conflated
        // StateFlow meant a CurrentPlaying update published immediately before a Playing
        // update (or just before the next 500ms Progress tick) was dropped before any
        // collector observed it, which left the "currently selected song" stale or unset.
        repositoryScope.launch {
            serviceHandler.currentIndex.collect { index ->
                _currentIndex.value = index
                persistCurrentPlaybackPosition()
                _activeQueue.value.getOrNull(index)?.let { song ->
                    // Best-effort - a play is logged whenever the current item changes.
                    // Doesn't attempt to distinguish "listened to the whole thing" from "skipped
                    // after 2 seconds"; that's a reasonable v1 for Recently Played / Most Played,
                    // and never blocks or delays actual playback since it's fire-and-forget on
                    // this same background-scoped coroutine.
                    libraryRepository.recordPlay(song.songId)
                }
            }
        }

        // Room is now the single source of truth for the song list (Home, queue, Search,
        // Artists/Albums/...). This block only keeps Room in sync with MediaStore:
        //  - first launch (empty table): scan immediately, the list can't show without it;
        //  - every later launch: the list already renders instantly from Room, so run a
        //    cheap incremental diff shortly AFTER it's on screen to pick up files added,
        //    removed or edited while the app was closed. (The ContentObserver only exists
        //    while the process is alive, so without this those changes were never seen.)
        repositoryScope.launch {
            val hasCachedLibrary = libraryRepository.observeTotalSongCount().first() > 0
            if (!hasCachedLibrary) {
                libraryRepository.scanLibrary()
            } else {
                initialQueueLoaded.await()
                delay(1_500)
                libraryRepository.scanLibrary(force = true)
            }
        }

        // Whenever playback starts, make sure the foreground service (and therefore the
        // notification + media session) is actually running. After the app is swiped out of
        // recents the service can be gone while ExoPlayer, held by this singleton, keeps
        // playing - nothing else restarted it until the user tapped a song.
        repositoryScope.launch {
            serviceHandler.isPlayingState.collect { playing ->
                if (playing) {
                    _sessionActive.value = true
                    runCatching { ensurePlaybackServiceStarted() }
                }
            }
        }

        repositoryScope.launch {
            serviceHandler.isPlayingState.collect { playing ->
                if (!playing) persistCurrentPlaybackPosition()
            }
        }

        repositoryScope.launch {
            serviceHandler.audioState.collect { state ->
                _playbackState.value = state.toDomain()
                if (state is MelodiqAudioState.Progress) {
                    // Throttle disk writes to ~every 5s while playing (10 ticks * 500ms),
                    // so a hard process kill mid-song still resumes close to where it left off.
                    progressTickCount++
                    if (progressTickCount % 10 == 0) persistCurrentPlaybackPosition()
                }
            }
        }

        // Single reactive pipeline for "load the whole library into the queue". This used
        // to be duplicated in every screen's ViewModel (Home, Player, Main), each doing its
        // own fetch and its own indexing into its own private copy of the list - if two of
        // those fetches ever returned subtly different orderings (a library change between
        // calls, a non-deterministic tie-break in the MediaStore query, etc.) the mini
        // player and the full player would end up pointing the same numeric index at two
        // different songs. Running it once, here, removes that class of bug entirely and
        // also cuts three redundant MediaStore queries + three redundant queue loads down
        // to one.
        repositoryScope.launch {
            preferencesDataStoreRepository.getSortType()
                .flatMapLatest { sortType ->
                    lastSortType = sortType
                    libraryRepository.observeAllSongs(sortType).map { songs -> sortType to songs }
                }
                .collect { (sortType, songs) ->
                    // An empty table just means "not scanned yet" (or permission not granted
                    // yet) - never replace a working queue with nothing.
                    if (songs.isEmpty()) return@collect
                    loadPlaylist(songs, sortType)
                    initialQueueLoaded.complete(Unit)
                }
        }

        // A file this app just renamed/moved/deleted/re-tagged: refresh Room immediately
        // instead of waiting on the debounced ContentObserver. The observeAllSongs flow above
        // then re-emits by itself and reloads the queue - no second fetch needed here.
        repositoryScope.launch {
            libraryChangeNotifier.changes.collectLatest {
                libraryRepository.scanLibrary(force = true)
            }
        }
    }


    private suspend fun persistCurrentPlaybackPosition() {
        // Never record anything while there is no active session (fresh install, or the user
        // closed the player) - otherwise index 0 would be saved as a "last played" song.
        if (!_sessionActive.value) return
        val songId = serviceHandler.audioList.value
            .getOrNull(serviceHandler.getCurrentMediaItemIndex())?.songId ?: return
        preferencesDataStoreRepository.saveLastPlayedTrack(songId, serviceHandler.getCurrentDuration())
    }

    override suspend fun loadPlaylist(musicList: List<MusicEntity>, sortType: SortType, keepCurrentTrack: Boolean) {
        // Build the MediaItems on a background thread first. Doing it inline on Main (for a
        // library of thousands of songs) froze the UI exactly when the Home list appeared.
        // Only the ExoPlayer calls below - which must be on Main - stay there, and the state
        // publish + player update still happen together with no suspension in between.
        val currentQueueIds = serviceHandler.audioList.value
        val queueUnchanged = serviceHandler.getMediaItemCount() > 0 && withContext(Dispatchers.Default) {
            currentQueueIds.size == musicList.size &&
                currentQueueIds.indices.all { currentQueueIds[it].songId == musicList[it].songId }
        }
        val mediaItems = if (queueUnchanged && keepCurrentTrack) emptyList()
        else withContext(Dispatchers.Default) { serviceHandler.buildMediaItems(musicList) }

        // Whatever list is loaded here becomes the authoritative queue that every screen's
        // "current song" lookup is based on - keep it in sync with what's actually handed
        // to ExoPlayer below.
        _audioList.value = musicList

        if (keepCurrentTrack && serviceHandler.getMediaItemCount() > 0) {
            // A playlist/curated queue is what's playing: a library refresh must only update
            // the browsable list, never yank the player's queue out from under it.
            if (curatedQueueActive) return
            // Same songs in the same order (e.g. Room re-emitted after an unrelated row
            // changed): nothing to reload, so playback isn't interrupted at all.
            if (queueUnchanged) {
                _activeQueue.value = musicList
                return
            }
        }

        _activeQueue.value = musicList
        curatedQueueActive = false
        if (!keepCurrentTrack) {
            serviceHandler.updateMediaItems(musicList, sortType, mediaItems)
            _currentIndex.value = serviceHandler.getCurrentMediaItemIndex()
            return
        }
        if (serviceHandler.getMediaItemCount() == 0) {
            val lastPlayed = preferencesDataStoreRepository.getLastPlayedTrack()
            serviceHandler.updateMediaItemsWithCurrentTrack(
                audioList = musicList,
                sortType = sortType,
                restoreSongId = lastPlayed?.songId,
                restorePositionMs = lastPlayed?.positionMs ?: 0L,
                prebuiltItems = mediaItems,
            )
            if (lastPlayed != null && musicList.any { it.songId == lastPlayed.songId }) {
                _sessionActive.value = true
            }
        } else {
            serviceHandler.updateMediaItemsWithCurrentTrack(musicList, sortType, prebuiltItems = mediaItems)
        }
    }

    // True when a curated list (e.g. a playlist) is loaded into the player instead of the
    // full library. Tracked explicitly rather than inferred by comparing list contents or
    // references, so the meaning stays obvious and can't silently break if either list is
    // ever copied rather than shared.
    private var curatedQueueActive = false

    override suspend fun playCuratedQueue(musicList: List<MusicEntity>, startIndex: Int) {
        // Deliberately does NOT touch _audioList - that's the browsable full library that
        // Home/Songs render, and must stay intact while a playlist (a smaller subset) is
        // what's actually loaded into the player.
        _activeQueue.value = musicList
        curatedQueueActive = true
        _sessionActive.value = true
        serviceHandler.playCuratedQueue(
            audioList = musicList,
            sortType = serviceHandler.sortType.value,
            startIndex = startIndex,
        )
    }

    override suspend fun playNext(song: MusicEntity) {
        serviceHandler.playNext(song)
        _activeQueue.value = serviceHandler.audioList.value
    }

    override suspend fun playLater(song: MusicEntity) {
        serviceHandler.playLater(song)
        _activeQueue.value = serviceHandler.audioList.value
    }

    override suspend fun addToQueue(songs: List<MusicEntity>) {
        serviceHandler.addToQueue(songs)
        _activeQueue.value = serviceHandler.audioList.value
    }

    override suspend fun removeFromQueue(index: Int) {
        serviceHandler.removeFromQueue(index)
        _activeQueue.value = serviceHandler.audioList.value
    }

    override suspend fun moveQueueItem(from: Int, to: Int) {
        serviceHandler.moveQueueItem(from, to)
        _activeQueue.value = serviceHandler.audioList.value
    }

    override suspend fun clearQueue() {
        serviceHandler.clearQueue()
        _activeQueue.value = emptyList()
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.SetPlaybackSpeed(speed))
    }

    override fun getPlaybackSpeed(): Float = serviceHandler.currentPlaybackSpeed()

    override fun setSeekStepMs(stepMs: Long) { serviceHandler.seekStepMs = stepMs }

    override fun getSeekStepMs(): Long = serviceHandler.seekStepMs

    override suspend fun play() {
        _sessionActive.value = true
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.Play)
    }

    override suspend fun pause() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.Pause)
    }

    override suspend fun fadeOutAndPause(durationMs: Long) {
        serviceHandler.fadeOutAndPause(durationMs)
    }

    override val visualizerBars: StateFlow<FloatArray> = serviceHandler.visualizerBars
    override val visualizerActive: StateFlow<Boolean> = serviceHandler.visualizerActive

    override suspend fun seekTo(position: Long) {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.SeekTo, seekPosition = position)
    }

    override suspend fun next() {
        _sessionActive.value = true
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.SkipNext)
    }

    override suspend fun previous() {
        _sessionActive.value = true
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.SkipPrevious)
    }

    override suspend fun forward() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.ForwardTrack5Sec)
    }

    override suspend fun backward() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.BackwardTrack5Sec)
    }

    override suspend fun getCurrentDuration(): Long = serviceHandler.getCurrentDuration()

    override suspend fun selectAudio(index: Int) {
        // If a curated queue (e.g. a playlist) is currently loaded instead of the full
        // library, an index from Home/Songs (which is always relative to the full library)
        // would be meaningless - or out of bounds - against that smaller queue. Restore the
        // library as the active queue first so the index means what the caller expects.
        if (curatedQueueActive) {
            serviceHandler.updateMediaItems(_audioList.value, serviceHandler.sortType.value)
            _activeQueue.value = _audioList.value
            curatedQueueActive = false
        }
        _sessionActive.value = true
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.SelectAudioChange, selectedAudionIndex = index)
    }

    override suspend fun stopPlayback() {
        // Order matters: drop the session first so nothing below (pause/seek triggers a
        // persist) can write the song back as "last played".
        _sessionActive.value = false
        serviceHandler.stopForClose()
        preferencesDataStoreRepository.clearLastPlayedTrack()
        closedByUser = true
        if (isPlaybackServiceRunning()) {
            runCatching {
                context.startService(
                    Intent(context, MelodiqPlayerService::class.java).setAction(MelodiqPlayerService.ACTION_CLOSE)
                )
            }
        }
    }

    override suspend fun updateProgress(progress: Float) {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.UpdateProgress(progress))
    }

    override suspend fun observeAudioState(): StateFlow<PlaybackState> = playbackState

    // Delegates to the shared, always-in-sync state instead of re-fetching the library
    // (which previously ignored the actual current sort type and could return a song
    // that didn't match the real current index).
    override suspend fun getCurrentSongInfo(): MusicEntity? = currentSelectedAudio.value

    override suspend fun getPlaybackSnapshot(): PlaybackSnapshot = PlaybackSnapshot(
        currentIndex = serviceHandler.getCurrentMediaItemIndex(),
        duration = serviceHandler.getDuration(),
        position = serviceHandler.getCurrentDuration(),
        isPlaying = serviceHandler.isPlaying(),
        mediaItemCount = serviceHandler.getMediaItemCount(),
        sortType = serviceHandler.sortType.value,
    )

    override fun isPlaybackServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == MelodiqPlayerService::class.java.name }
    }

    // True after "close player" took the notification down while leaving the service object
    // running, so the next playback must explicitly bring the notification back.
    private var closedByUser = false

    override fun ensurePlaybackServiceStarted() {
        if (closedByUser) {
            // The service is still running after "close player"; Media3 puts the notification
            // back by itself as soon as playback starts, so nothing needs (re)starting.
            closedByUser = false
            if (isPlaybackServiceRunning()) return
        }
        if (!isPlaybackServiceRunning()) {
            val intent = Intent(context, MelodiqPlayerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override suspend fun repeatTrackOne() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.RepeatTrackOne)
    }

    override suspend fun repeatTrackAll() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.RepeatTrackALl)
    }

    override suspend fun repeatTrackOff() {
        serviceHandler.onPlayerEvents(MelodiqPlayerEvent.RepeatTrackOff)
    }
}

private fun MelodiqAudioState.toDomain(): PlaybackState = when (this) {
    MelodiqAudioState.Initial -> PlaybackState.Idle
    is MelodiqAudioState.Ready -> PlaybackState.Ready(duration)
    is MelodiqAudioState.Progress -> PlaybackState.Progress(progress)
    is MelodiqAudioState.Buffering -> PlaybackState.Buffering(progress)
    is MelodiqAudioState.Playing -> PlaybackState.Playing(isPlaying)
    is MelodiqAudioState.CurrentPlaying -> PlaybackState.TrackChanged(mediaItemIndex)
}