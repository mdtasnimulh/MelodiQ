package com.tasnimulhasan.melodiq.ipod

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.library.LibraryUseCases
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.localusecase.playlistdetails.GetAllMusicFromPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.GetAllPlaylistUseCase
import com.tasnimulhasan.domain.player.PlaybackState
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.library.AlbumSummary
import com.tasnimulhasan.entity.room.library.ArtistSummary
import com.tasnimulhasan.entity.room.library.GenreSummary
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State and actions behind the iPod-style UI (Player B). It reads the very same shared player
 * and library state as the regular app, so switching between the two never changes what is
 * playing - only how it is shown and controlled.
 */
@HiltViewModel
internal class IpodViewModel @Inject constructor(
    private val playerUseCases: PlayerUseCases,
    private val libraryUseCases: LibraryUseCases,
    private val getAllPlaylistUseCase: GetAllPlaylistUseCase,
    private val getAllMusicFromPlaylistUseCase: GetAllMusicFromPlaylistUseCase,
    observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // ─── Navigation stack ────────────────────────────────────────────────────────────────
    val stack = mutableStateListOf(IpodEntry(IpodPage.MAIN, "MelodiQ", depth = 0))

    fun push(page: IpodPage, title: String, songs: List<MusicEntity> = emptyList()) {
        // Never stack two Now Playing screens (e.g. tapping a song while already on it).
        if (page == IpodPage.NOW_PLAYING && stack.last().page == IpodPage.NOW_PLAYING) return
        stack.add(IpodEntry(page, title, depth = stack.size, songs = songs))
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun goToMain() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    // ─── Library + player state (shared with the regular UI) ─────────────────────────────
    val songs: StateFlow<List<MusicEntity>> = playerUseCases.observeAudioList()
    val current: StateFlow<MusicEntity?> = playerUseCases.observeCurrentSelectedAudio()
    val isPlaying: StateFlow<Boolean> = playerUseCases.observeIsPlaying()

    val artists: StateFlow<List<ArtistSummary>> = libraryUseCases.observeArtists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val albums: StateFlow<List<AlbumSummary>> = libraryUseCases.observeAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val genres: StateFlow<List<GenreSummary>> = libraryUseCases.observeGenres()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = getAllPlaylistUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favouriteIds: StateFlow<Set<Long>> = observeFavouriteIdsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // ─── Opening sub-lists ───────────────────────────────────────────────────────────────
    fun openArtist(name: String) {
        viewModelScope.launch { push(IpodPage.SONG_LIST, name, libraryUseCases.getSongsByArtist(name)) }
    }

    fun openAlbum(albumId: Long, title: String) {
        viewModelScope.launch { push(IpodPage.SONG_LIST, title, libraryUseCases.getSongsByAlbum(albumId)) }
    }

    fun openGenre(genre: String) {
        viewModelScope.launch { push(IpodPage.SONG_LIST, genre, libraryUseCases.getSongsByGenre(genre)) }
    }

    fun openPlaylist(playlistId: Int, name: String) {
        viewModelScope.launch {
            val saved = getAllMusicFromPlaylistUseCase.invoke(params = GetAllMusicFromPlaylistUseCase.Params(playlistId)).first()
            // Resolve against the live library so deleted files drop out and tags are current.
            val library = songs.value.associateBy { it.songId }
            push(IpodPage.SONG_LIST, name, saved.mapNotNull { library[it.songId] })
        }
    }

    // ─── Playback ────────────────────────────────────────────────────────────────────────
    fun playFromLibrary(song: MusicEntity) {
        val index = songs.value.indexOfFirst { it.songId == song.songId }
        if (index < 0) return
        playerUseCases.ensurePlaybackServiceStarted()
        viewModelScope.launch { playerUseCases.selectAudioChange(index) }
    }

    fun playQueue(list: List<MusicEntity>, index: Int) {
        if (list.isEmpty()) return
        playerUseCases.ensurePlaybackServiceStarted()
        viewModelScope.launch { playerUseCases.playCuratedQueue(list, index.coerceIn(0, list.lastIndex)) }
    }

    fun shuffleAll() {
        val list = songs.value
        if (list.isEmpty()) return
        playQueue(list.shuffled(), 0)
    }

    fun playPause() {
        viewModelScope.launch {
            when {
                isPlaying.value -> playerUseCases.pause()
                current.value == null -> songs.value.firstOrNull()?.let { playFromLibrary(it) }
                else -> playerUseCases.play()
            }
        }
    }

    fun next() { viewModelScope.launch { playerUseCases.next() } }
    fun previous() { viewModelScope.launch { playerUseCases.previous() } }

    /** Called repeatedly while next/previous is held on the wheel. */
    fun seekHold(forward: Boolean) {
        viewModelScope.launch {
            if (forward) playerUseCases.forwardTrackUseCase() else playerUseCases.backwardTrackUseCase()
        }
    }

    // ─── Now Playing: progress, volume, scrubbing ────────────────────────────────────────
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    /** true: the wheel scrubs through the song; false: the wheel changes the volume. */
    private val _scrubbing = MutableStateFlow(false)
    val scrubbing: StateFlow<Boolean> = _scrubbing.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

    private val _volume = MutableStateFlow(readVolume())
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _showVolume = MutableStateFlow(false)
    val showVolume: StateFlow<Boolean> = _showVolume.asStateFlow()
    private var hideVolumeJob: Job? = null

    private fun readVolume(): Float =
        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume

    init {
        viewModelScope.launch {
            playerUseCases.observeAudioState().collectLatest { state ->
                when (state) {
                    is PlaybackState.Progress -> _positionMs.value = state.position
                    is PlaybackState.Buffering -> _positionMs.value = state.position
                    is PlaybackState.Ready -> {
                        _durationMs.value = state.duration
                        _positionMs.value = playerUseCases.getPlaybackSnapshot().position
                    }
                    else -> Unit
                }
            }
        }
        viewModelScope.launch {
            current.collectLatest { song ->
                _scrubbing.value = false
                val d = song?.duration?.toLongOrNull()
                if (d != null && d > 0) _durationMs.value = d
                if (song == null) _positionMs.value = 0L
            }
        }
        viewModelScope.launch {
            val snapshot = playerUseCases.getPlaybackSnapshot()
            _durationMs.value = snapshot.duration
            _positionMs.value = snapshot.position
        }
    }

    fun toggleScrub() {
        if (current.value != null) _scrubbing.value = !_scrubbing.value
    }

    /** Wheel turned on the Now Playing screen: scrub when in scrub mode, else change volume. */
    fun nowPlayingRotate(steps: Int) {
        if (_scrubbing.value) scrub(steps) else adjustVolume(steps)
    }

    private fun scrub(steps: Int) {
        val duration = _durationMs.value
        if (duration <= 0L) return
        val step = (duration / 60).coerceAtLeast(1_000L)
        val target = (_positionMs.value + steps * step).coerceIn(0L, duration)
        _positionMs.value = target
        viewModelScope.launch { playerUseCases.seekTo(target) }
    }

    private fun adjustVolume(steps: Int) {
        try {
            val index = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + steps).coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
        } catch (_: SecurityException) {
            // Do Not Disturb policies can refuse volume changes; nothing useful to do.
        }
        _volume.value = readVolume()
        _showVolume.value = true
        hideVolumeJob?.cancel()
        hideVolumeJob = viewModelScope.launch {
            delay(1_800)
            _showVolume.value = false
        }
    }

    // ─── Small iPod-only preference ──────────────────────────────────────────────────────
    private val prefs = context.getSharedPreferences("ipod_ui", Context.MODE_PRIVATE)

    private val _clickFeedback = MutableStateFlow(prefs.getBoolean(KEY_CLICK, true))
    /** Wheel ticks play the system click sound and a light vibration when on. */
    val clickFeedback: StateFlow<Boolean> = _clickFeedback.asStateFlow()

    fun toggleClickFeedback() {
        val next = !_clickFeedback.value
        _clickFeedback.value = next
        prefs.edit().putBoolean(KEY_CLICK, next).apply()
    }

    private companion object {
        const val KEY_CLICK = "click_feedback"
    }
}
