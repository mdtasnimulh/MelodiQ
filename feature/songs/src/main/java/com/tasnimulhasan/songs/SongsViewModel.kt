package com.tasnimulhasan.songs

import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.datastore.GetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ToggleFavouriteUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongsViewModel @Inject constructor(
    private val playerUseCases: PlayerUseCases,
    private val getSortTypeUseCase: GetSortTypeUseCase,
    private val setSortTypeUseCase: SetSortTypeUseCase,
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

    private val _sortType = MutableStateFlow(SortType.DATE_MODIFIED_DESC)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    // Same shared, single-source-of-truth state everything else reads - this list is
    // guaranteed to be identical to what Home/the mini player/the full player show.
    val audioList: StateFlow<List<MusicEntity>> = playerUseCases.observeAudioList()

    val currentSelectedAudio: StateFlow<MusicEntity> = playerUseCases.observeCurrentSelectedAudio()
        .map { it ?: dummyAudio }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), dummyAudio)

    val isPlaying: StateFlow<Boolean> = playerUseCases.observeIsPlaying()

    val favorites: StateFlow<Set<Long>> = observeFavouriteIdsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            getSortTypeUseCase().collectLatest { _sortType.value = it }
        }
    }

    fun setSortType(type: SortType) {
        viewModelScope.launch { setSortTypeUseCase(type) }
    }

    fun playSong(songId: Long) {
        val index = audioList.value.indexOfFirst { it.songId == songId }
        if (index >= 0) viewModelScope.launch { playerUseCases.selectAudioChange(index) }
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
        viewModelScope.launch { toggleFavouriteUseCase(songId) }
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

    fun ensurePlaybackServiceStarted() = playerUseCases.ensurePlaybackServiceStarted()
}
