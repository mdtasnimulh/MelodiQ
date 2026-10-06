package com.tasnimulhasan.home

import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.datastore.GetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ToggleFavouriteUseCase
import com.tasnimulhasan.domain.localusecase.library.LibraryUseCases
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.localusecase.playlistdetails.InsertMusicListToPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlistdetails.InsertMusicToPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlistdetails.IsSongInPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.DeletePlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.GetAllPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.InsertPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.SearchPlaylistByNameUseCase
import com.tasnimulhasan.domain.localusecase.playlists.UpdatePlaylistUseCase
import com.tasnimulhasan.domain.player.PlaybackState
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import com.tasnimulhasan.entity.room.library.ListeningStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val playerUseCases: PlayerUseCases,
    private val libraryUseCases: LibraryUseCases,
    private val setSortTypeUseCase: SetSortTypeUseCase,
    private val getSortTypeUseCase: GetSortTypeUseCase,
    private val getAllPlaylistUseCase: GetAllPlaylistUseCase,
    private val insertMusicToPlaylist: InsertMusicToPlaylistUseCase,
    private val isSongInPlaylistUseCase: IsSongInPlaylistUseCase,
    private val insertMusicListToPlaylistUseCase: InsertMusicListToPlaylistUseCase,
    private val observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
) : BaseViewModel() {

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

    var initializedList = MutableStateFlow(false)

    private val _sortType = MutableStateFlow(SortType.DATE_MODIFIED_DESC)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _progressString = MutableStateFlow("00:00")
    val progressString = _progressString.asStateFlow()

    // Bound directly to the shared play/pause state rather than a local copy fed by
    // PlaybackState.Playing - that signal travelled on a conflated StateFlow and could be
    // dropped, leaving the play/pause button out of sync with reality.
    val isPlaying: StateFlow<Boolean> = playerUseCases.observeIsPlaying()

    // Pass-throughs onto the repository's single shared StateFlow - see PlayerRepositoryImpl.
    // The home list must read exactly the same song identity the mini player and full
    // player do, or the "now playing" highlight on a row can point at the wrong song.
    val audioList: StateFlow<List<MusicEntity>> = playerUseCases.observeAudioList()

    val currentSelectedAudio: StateFlow<MusicEntity> = playerUseCases.observeCurrentSelectedAudio()
        .map { it ?: dummyAudio }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), dummyAudio)

    private val _uIState: MutableStateFlow<UIState> = MutableStateFlow(UIState.Initial)
    val uIState: StateFlow<UIState> = _uIState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent get() = _uiEvent.receiveAsFlow()

    val favorites: StateFlow<Set<Long>> = observeFavouriteIdsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // --- Dashboard data for the redesigned Home screen -----------------------------------
    // All derived from the existing shared library/history infrastructure - nothing here
    // duplicates or races the Songs screen's own copy of the same underlying data.

    val listeningStats: StateFlow<ListeningStats> = libraryUseCases.observeListeningStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListeningStats(0, 0L))

    val artistCount: StateFlow<Int> = libraryUseCases.observeArtists()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val albumCount: StateFlow<Int> = libraryUseCases.observeAlbums()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val recentlyPlayed: StateFlow<List<Pair<MusicEntity, Long>>> = libraryUseCases.observeRecentlyPlayedWithTimestamp(12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mostPlayed: StateFlow<List<Pair<MusicEntity, Int>>> = libraryUseCases.observeMostPlayed(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Favorite songs resolved against the same audioList everything else reads, rather than
    // a second query - a favorite that isn't in the current library scan just doesn't show,
    // same as everywhere else in the app.
    val favoriteSongs: StateFlow<List<MusicEntity>> = combine(favorites, audioList) { ids, songs ->
        songs.filter { ids.contains(it.songId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val action: (UiAction) -> Unit = {
        when (it) {
            is UiAction.FetchAllPlaylists -> fetchAllPlaylists()
            is UiAction.AddMusicToPlaylist -> addMusicToPlaylist(playlistId = it.playlistId, music = it.music)
            is UiAction.ToggleFavorite -> toggleFavorite(it.songId)
        }
    }

    init {
        viewModelScope.launch {
            getSortTypeUseCase().collectLatest { persistedSortType ->
                _sortType.value = persistedSortType
            }
        }

        viewModelScope.launch {
            playerUseCases.observeAudioState().collectLatest { mediaState ->
                when (mediaState) {
                    PlaybackState.Idle -> _uIState.value = UIState.Initial
                    is PlaybackState.Buffering -> calculateProgressValue(mediaState.position)
                    is PlaybackState.Playing -> Unit
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
    }

    fun setSortType(type: SortType) {
        viewModelScope.launch {
            _sortType.value = type
            // Just persist the preference - the repository's own reactive pipeline picks
            // this up, re-fetches the library once, and reloads the queue. Doing it again
            // here too would race the same fetch against itself.
            setSortTypeUseCase(type)
            initializedList.value = true
        }
    }

    fun onUiEvents(uiEvents: UIEvents) = viewModelScope.launch {
        when (uiEvents) {
            is UIEvents.Backward -> playerUseCases.backwardTrackUseCase()
            is UIEvents.Forward -> playerUseCases.forwardTrackUseCase()
            is UIEvents.PlayPause -> {
                if (isPlaying.value) playerUseCases.pause() else playerUseCases.play()
            }
            is UIEvents.SeekTo -> {
                val position = ((_duration.value * uiEvents.position) / 100f).toLong()
                playerUseCases.seekTo(position)
            }
            UIEvents.SeekToNext -> playerUseCases.next()
            is UIEvents.SelectedAudioChange -> playerUseCases.selectAudioChange(uiEvents.index)
            is UIEvents.UpdateProgress -> playerUseCases.updateProgress(uiEvents.newProgress)
            UIEvents.SeekToPrevious -> playerUseCases.previous()
        }
    }

    private fun calculateProgressValue(currentProgress: Long) {
        _progress.value =
            if (currentProgress > 0 && _duration.value > 0) ((currentProgress.toFloat() / _duration.value.toFloat()) * 100f)
            else 0f
        _progressString.value = formatDuration(currentProgress)
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

    /** "2 min ago" / "3h ago" / "Yesterday" / "12 Aug" style relative label for Recently
     * Played - not the exact timestamp the user doesn't need. */
    fun relativeTimeAgo(epochMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = (now - epochMillis).coerceAtLeast(0L)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> SimpleDateFormat("d MMM", Locale.US).format(epochMillis)
        }
    }

    fun sortTypeToDisplayString(sortType: SortType): String = when (sortType) {
        SortType.DATE_MODIFIED_ASC -> "Date Modified (ASC)"
        SortType.DATE_MODIFIED_DESC -> "Date Modified (DESC)"
        SortType.NAME_ASC -> "Name (ASC)"
        SortType.NAME_DESC -> "Name (DESC)"
        SortType.ARTIST_ASC -> "Artist (ASC)"
        SortType.ARTIST_DESC -> "Artist (DESC)"
        SortType.DURATION_ASC -> "Duration (ASC)"
        SortType.DURATION_DESC -> "Duration (DESC)"
    }

    private fun fetchAllPlaylists() {
        execute {
            _uiEvent.send(UiEvent.Loading(true))
            getAllPlaylistUseCase.invoke().collect {
                _uiEvent.send(UiEvent.Loading(false))
                if (it.isEmpty()) _uiEvent.send(UiEvent.DataEmpty)
                else _uiEvent.send(UiEvent.Playlists(it))
            }
        }
    }

    private fun addMusicToPlaylist(playlistId: Int, music: MusicEntity) {
        execute {
            val alreadyAdded = isSongInPlaylistUseCase(
                IsSongInPlaylistUseCase.Params(playlistId = playlistId, songId = music.songId)
            )
            if (alreadyAdded) {
                _uiEvent.send(UiEvent.ShowToast("Already added to this playlist"))
                return@execute
            }
            val details = PlaylistDetailsEntity(
                playlistId = playlistId,
                contentUri = music.contentUri.toString(),
                songId = music.songId,
                cover = music.contentUri.toString(),
                songTitle = music.songTitle,
                artist = music.artist,
                duration = music.duration,
                album = music.album,
                albumId = music.albumId
            )
            insertMusicToPlaylist(params = InsertMusicToPlaylistUseCase.Params(details))
            _uiEvent.send(UiEvent.ShowToast("Added to playlist"))
        }
    }

    fun playNext(song: MusicEntity) = viewModelScope.launch {
        playerUseCases.playNext(song)
        _uiEvent.send(UiEvent.ShowToast("Playing next"))
    }

    fun playLater(song: MusicEntity) = viewModelScope.launch {
        playerUseCases.playLater(song)
        _uiEvent.send(UiEvent.ShowToast("Added to queue"))
    }

    fun playAll() {
        if (audioList.value.isEmpty()) return
        viewModelScope.launch { playerUseCases.selectAudioChange(0) }
    }

    fun shuffleAll() {
        val indices = audioList.value.indices
        if (indices.isEmpty()) return
        viewModelScope.launch { playerUseCases.selectAudioChange(indices.random()) }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            toggleFavouriteUseCase(songId)
        }
    }
    fun isPlaybackServiceRunning(): Boolean = playerUseCases.isPlaybackServiceRunning()
    fun ensurePlaybackServiceStarted() = playerUseCases.ensurePlaybackServiceStarted()
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
}

sealed class UIState {
    data class MusicList(val musics: List<MusicEntity>) : UIState()
    data object Initial : UIState()
    data object Ready : UIState()
}

sealed interface UiEvent {
    data class Loading(val loading: Boolean) : UiEvent
    data class ShowToast(val message: String) : UiEvent
    data object DataEmpty : UiEvent
    data class Playlists(val playlists: List<PlaylistEntity>) : UiEvent
}

sealed interface UiAction {
    data object FetchAllPlaylists : UiAction
    data class AddMusicToPlaylist(val playlistId: Int, val music: MusicEntity) : UiAction
    data class ToggleFavorite(val songId: Long) : UiAction
}