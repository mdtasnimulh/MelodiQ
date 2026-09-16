package com.tasnimulhasan.featurefavourite

import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ToggleFavouriteUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouriteViewModel @Inject constructor(
    private val playerUseCases: PlayerUseCases,
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

    // Favourited songs, resolved against the SAME shared library list everything else
    // reads - never a separate query, and the order matches the library's current sort.
    val favouriteSongs: StateFlow<List<MusicEntity>> = combine(
        playerUseCases.observeAudioList(),
        observeFavouriteIdsUseCase(),
    ) { allSongs, favouriteIds ->
        allSongs.filter { favouriteIds.contains(it.songId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentSelectedAudio: StateFlow<MusicEntity> = playerUseCases.observeCurrentSelectedAudio()
        .map { it ?: dummyAudio }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), dummyAudio)

    val isPlaying: StateFlow<Boolean> = playerUseCases.observeIsPlaying()

    fun playSong(songId: Long) {
        val index = playerUseCases.observeAudioList().value.indexOfFirst { it.songId == songId }
        if (index >= 0) viewModelScope.launch { playerUseCases.selectAudioChange(index) }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { toggleFavouriteUseCase(songId) }
    }

    fun ensurePlaybackServiceStarted() = playerUseCases.ensurePlaybackServiceStarted()
}
