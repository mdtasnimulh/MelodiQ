package com.tasnimulhasan.library

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ToggleFavouriteUseCase
import com.tasnimulhasan.domain.localusecase.library.LibraryUseCases
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.library.ArtistSummary
import com.tasnimulhasan.entity.room.library.GenreSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal val dummyAudio = MusicEntity(
    contentUri = "".toUri(),
    songId = 0L,
    cover = null,
    songTitle = "",
    artist = "",
    duration = "",
    albumId = 0L,
    album = ""
)

/** Common "now playing + favourites" wiring shared by every Library sub-screen ViewModel. */
class LibraryPlaybackState(
    private val playerUseCases: PlayerUseCases,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
    observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
    private val scope: CoroutineScope,
) {
    val currentSelectedAudio: StateFlow<MusicEntity> = playerUseCases.observeCurrentSelectedAudio()
        .map { it ?: dummyAudio }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), dummyAudio)

    val isPlaying: StateFlow<Boolean> = playerUseCases.observeIsPlaying()

    val favorites: StateFlow<Set<Long>> = observeFavouriteIdsUseCase()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleFavorite(songId: Long) {
        scope.launch { toggleFavouriteUseCase(songId) }
    }

    /** Plays [songId] against the full shared library queue - resolves its real index there
     * first, the same safe pattern Home/Songs/AlbumDetails use, rather than trusting an
     * index from whatever smaller/filtered list the calling screen is showing. */
    fun playFromLibrary(songId: Long) {
        val index = playerUseCases.observeAudioList().value.indexOfFirst { it.songId == songId }
        if (index >= 0) scope.launch { playerUseCases.selectAudioChange(index) }
    }

    fun ensurePlaybackServiceStarted() = playerUseCases.ensurePlaybackServiceStarted()
}

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    libraryUseCases: LibraryUseCases,
) : ViewModel() {
    val artists: StateFlow<List<ArtistSummary>> = libraryUseCases.observeArtists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@HiltViewModel
class ArtistDetailsViewModel @Inject constructor(
    private val libraryUseCases: LibraryUseCases,
    playerUseCases: PlayerUseCases,
    toggleFavouriteUseCase: ToggleFavouriteUseCase,
    observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
) : ViewModel() {
    private val artist: String = savedStateHandle.get<String>("artist") ?: ""

    val playback = LibraryPlaybackState(playerUseCases, toggleFavouriteUseCase, observeFavouriteIdsUseCase, viewModelScope)

    private val _songs = MutableStateFlow<List<MusicEntity>>(emptyList())
    val songs: StateFlow<List<MusicEntity>> = _songs.asStateFlow()

    init {
        viewModelScope.launch {
            _songs.value = libraryUseCases.getSongsByArtist(artist)
        }
    }
}

@HiltViewModel
class GenresViewModel @Inject constructor(
    libraryUseCases: LibraryUseCases,
) : ViewModel() {
    val genres: StateFlow<List<GenreSummary>> = libraryUseCases.observeGenres()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@HiltViewModel
class GenreDetailsViewModel @Inject constructor(
    private val libraryUseCases: LibraryUseCases,
    playerUseCases: PlayerUseCases,
    toggleFavouriteUseCase: ToggleFavouriteUseCase,
    observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
) : ViewModel() {
    private val genre: String = savedStateHandle.get<String>("genre") ?: ""

    val playback = LibraryPlaybackState(playerUseCases, toggleFavouriteUseCase, observeFavouriteIdsUseCase, viewModelScope)

    private val _songs = MutableStateFlow<List<MusicEntity>>(emptyList())
    val songs: StateFlow<List<MusicEntity>> = _songs.asStateFlow()

    init {
        viewModelScope.launch {
            _songs.value = libraryUseCases.getSongsByGenre(genre)
        }
    }
}

@HiltViewModel
class RecentlyPlayedViewModel @Inject constructor(
    libraryUseCases: LibraryUseCases,
    playerUseCases: PlayerUseCases,
    toggleFavouriteUseCase: ToggleFavouriteUseCase,
    observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
) : ViewModel() {
    val playback = LibraryPlaybackState(playerUseCases, toggleFavouriteUseCase, observeFavouriteIdsUseCase, viewModelScope)

    val recentlyPlayed: StateFlow<List<MusicEntity>> = libraryUseCases.observeRecentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mostPlayed: StateFlow<List<Pair<MusicEntity, Int>>> = libraryUseCases.observeMostPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
