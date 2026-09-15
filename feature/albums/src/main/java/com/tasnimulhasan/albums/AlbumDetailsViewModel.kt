package com.tasnimulhasan.albums

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ToggleFavouriteUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailsViewModel @Inject constructor(
    private val playerUseCases: PlayerUseCases,
    private val observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel() {

    private val albumId: Long = savedStateHandle.get<Long>("albumId") ?: -1L

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

    // Filtered from the SAME shared list everything else uses - never a second query.
    val songs: StateFlow<List<MusicEntity>> = playerUseCases.observeAudioList()
        .map { all -> all.filter { it.albumId == albumId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albumName: StateFlow<String> = songs.map { it.firstOrNull()?.album ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val currentSelectedAudio: StateFlow<MusicEntity> = playerUseCases.observeCurrentSelectedAudio()
        .map { it ?: dummyAudio }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), dummyAudio)

    val isPlaying: StateFlow<Boolean> = playerUseCases.observeIsPlaying()

    val favorites: StateFlow<Set<Long>> = observeFavouriteIdsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun playSong(songId: Long) {
        // Important: select against the index in the FULL shared library, not the index
        // within this filtered album sub-list - the player's queue is the full library,
        // so an index only means the right thing there.
        val globalIndex = playerUseCases.observeAudioList().value.indexOfFirst { it.songId == songId }
        if (globalIndex >= 0) viewModelScope.launch { playerUseCases.selectAudioChange(globalIndex) }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { toggleFavouriteUseCase(songId) }
    }

    fun ensurePlaybackServiceStarted() = playerUseCases.ensurePlaybackServiceStarted()
}
