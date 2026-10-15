package com.tasnimulhasan.melodiq.ipod

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ToggleFavouriteUseCase
import com.tasnimulhasan.domain.localusecase.playlists.InsertPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlistdetails.InsertMusicToPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlistdetails.IsSongInPlaylistUseCase
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
    private val insertMusicToPlaylistUseCase: InsertMusicToPlaylistUseCase,
    private val isSongInPlaylistUseCase: IsSongInPlaylistUseCase,
    private val insertPlaylistUseCase: InsertPlaylistUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // ─── Navigation ──────────────────────────────────────────────────────────────────────
    // Two stacks: what fills the display (starts on the Cover Flow of every song) and the
    // menu panel that slides in over it. The panel is only there after MENU is pressed.
    val content = mutableStateListOf(IpodEntry(IpodPage.HOME, "Cover Flow", depth = 0))
    val menu = mutableStateListOf<IpodEntry>()

    fun openMenu() {
        if (menu.isEmpty()) menu.add(IpodEntry(IpodPage.MAIN, "MelodiQ", depth = 0))
    }

    fun closeMenu() = menu.clear()

    /** Opens the menu panel directly on [page] (MENU then closes it in one press). */
    fun openMenuPage(page: IpodPage, title: String) {
        menu.clear()
        menu.add(IpodEntry(page, title, depth = 0))
    }

    fun pushMenu(page: IpodPage, title: String) {
        menu.add(IpodEntry(page, title, depth = menu.size))
    }

    /**
     * Shows a content page and closes the menu. [fromRoot] first drops back to the home Cover
     * Flow, so a page chosen from the menu doesn't pile on top of whatever was open before.
     */
    fun pushContent(page: IpodPage, title: String, songs: List<MusicEntity> = emptyList(), fromRoot: Boolean = false) {
        closeMenu()
        if (fromRoot) while (content.size > 1) content.removeAt(content.lastIndex)
        // Never stack two Now Playing screens (e.g. picking a song while already on it).
        if (page == IpodPage.NOW_PLAYING && content.last().page == IpodPage.NOW_PLAYING) return
        content.add(IpodEntry(page, title, depth = content.size, songs = songs))
    }

    fun showHome() {
        closeMenu()
        while (content.size > 1) content.removeAt(content.lastIndex)
    }

    /**
     * The MENU button. Menu open: go up a menu level, or close the menu from its first level.
     * Menu closed: go back a screen, or - from the home Cover Flow - open the menu.
     */
    fun menuButton() {
        when {
            menu.isNotEmpty() -> if (menu.size > 1) menu.removeAt(menu.lastIndex) else closeMenu()
            content.size > 1 -> content.removeAt(content.lastIndex)
            else -> openMenu()
        }
    }

    /** Holding MENU opens the menu from anywhere, however deep the current screen is. */
    fun menuHold() = openMenu()

    /** System back: same as MENU, but lets the app close when already at the home Cover Flow. */
    fun back(): Boolean {
        if (menu.isEmpty() && content.size <= 1) return false
        menuButton()
        return true
    }

    private var homeSynced = false

    /** Once the library has loaded, start the home Cover Flow on the song that is current. */
    fun syncHomeSelection() {
        if (homeSynced) return
        val list = songs.value
        if (list.isEmpty()) return
        homeSynced = true
        val id = current.value?.songId ?: return
        val index = list.indexOfFirst { it.songId == id }
        if (index >= 0) content.first().selected = index
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
        viewModelScope.launch { pushContent(IpodPage.SONG_LIST, name, libraryUseCases.getSongsByArtist(name)) }
    }

    fun openAlbum(albumId: Long, title: String) {
        viewModelScope.launch { pushContent(IpodPage.SONG_LIST, title, libraryUseCases.getSongsByAlbum(albumId)) }
    }

    fun openGenre(genre: String) {
        viewModelScope.launch { pushContent(IpodPage.SONG_LIST, genre, libraryUseCases.getSongsByGenre(genre)) }
    }

    fun openPlaylist(playlistId: Int, name: String) {
        viewModelScope.launch {
            val saved = getAllMusicFromPlaylistUseCase.invoke(params = GetAllMusicFromPlaylistUseCase.Params(playlistId)).first()
            // Resolve against the live library so deleted files drop out and tags are current.
            val library = songs.value.associateBy { it.songId }
            pushContent(IpodPage.SONG_LIST, name, saved.mapNotNull { library[it.songId] }, fromRoot = true)
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
        // Opened while music is already playing: land on Now Playing (MENU goes back to the Cover Flow).
        if (isPlaying.value && current.value != null) {
            content.add(IpodEntry(IpodPage.NOW_PLAYING, "Now Playing", depth = 1))
        }
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

    // ─── Favourite / playlist actions for the current song ───────────────────────────────
    private val _messages = Channel<String>(Channel.BUFFERED)
    /** One-off messages (toasts) for the UI. */
    val messages = _messages.receiveAsFlow()

    fun toggleFavouriteCurrent() {
        val song = current.value ?: return
        viewModelScope.launch { toggleFavouriteUseCase(song.songId) }
    }

    fun addCurrentToPlaylist(playlist: PlaylistEntity) {
        val song = current.value ?: return
        viewModelScope.launch {
            val already = isSongInPlaylistUseCase(IsSongInPlaylistUseCase.Params(playlist.id, song.songId))
            if (already) {
                _messages.send("Already in ${playlist.playlistName}")
                return@launch
            }
            insertMusicToPlaylistUseCase(
                InsertMusicToPlaylistUseCase.Params(
                    PlaylistDetailsEntity(
                        playlistId = playlist.id,
                        contentUri = song.contentUri.toString(),
                        songId = song.songId,
                        cover = song.contentUri.toString(),
                        songTitle = song.songTitle,
                        artist = song.artist,
                        duration = song.duration,
                        album = song.album,
                        albumId = song.albumId,
                    )
                )
            )
            _messages.send("Added to ${playlist.playlistName}")
        }
    }

    /** Creates a playlist called [name]; with [addCurrent] the playing song goes straight into it. */
    fun createPlaylist(name: String, addCurrent: Boolean) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val createdAt = System.currentTimeMillis()
            insertPlaylistUseCase(
                InsertPlaylistUseCase.Params(
                    PlaylistEntity(playlistName = clean, playlistDescription = "", createdAt = createdAt)
                )
            )
            if (!addCurrent) {
                _messages.send("Playlist \"$clean\" created")
                return@launch
            }
            // insertPlaylist returns nothing, so find the new row (newest with this name).
            val created = getAllPlaylistUseCase().first()
                .filter { it.playlistName == clean && it.createdAt == createdAt }
                .maxByOrNull { it.id }
            if (created != null) addCurrentToPlaylist(created) else _messages.send("Couldn't add to playlist")
        }
    }

    // ─── Leaving the app ─────────────────────────────────────────────────────────────────
    /** Stops playback and the playback service, then runs [afterStopped] (which closes the app). */
    fun stopEverything(afterStopped: () -> Unit) {
        viewModelScope.launch {
            withContext(NonCancellable) { runCatching { playerUseCases.stopPlayback() } }
            delay(250)
            afterStopped()
        }
    }

    // ─── Small iPod-only preference ──────────────────────────────────────────────────────
    private val prefs = context.getSharedPreferences("ipod_ui", Context.MODE_PRIVATE)

    private val _clickFeedback = MutableStateFlow(prefs.getBoolean(KEY_CLICK, true))
    /** Wheel ticks play the system click sound and a light vibration when on. */
    val clickFeedback: StateFlow<Boolean> = _clickFeedback.asStateFlow()

    private val _color = MutableStateFlow(
        runCatching { IpodColor.valueOf(prefs.getString(KEY_COLOR, IpodColor.WHITE.name)!!) }.getOrDefault(IpodColor.WHITE)
    )
    val color: StateFlow<IpodColor> = _color.asStateFlow()

    fun setColor(color: IpodColor) {
        _color.value = color
        prefs.edit().putString(KEY_COLOR, color.name).apply()
    }

    fun toggleClickFeedback() {
        val next = !_clickFeedback.value
        _clickFeedback.value = next
        prefs.edit().putBoolean(KEY_CLICK, next).apply()
    }

    private companion object {
        const val KEY_CLICK = "click_feedback"
        const val KEY_COLOR = "body_color"
    }
}
