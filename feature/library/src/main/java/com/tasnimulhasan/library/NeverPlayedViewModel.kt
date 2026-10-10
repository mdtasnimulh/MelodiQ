package com.tasnimulhasan.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ToggleFavouriteUseCase
import com.tasnimulhasan.domain.localusecase.library.ObserveNeverPlayedUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NeverPlayedViewModel @Inject constructor(
    observeNeverPlayedUseCase: ObserveNeverPlayedUseCase,
    playerUseCases: PlayerUseCases,
    toggleFavouriteUseCase: ToggleFavouriteUseCase,
    observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
) : ViewModel() {
    val playback = LibraryPlaybackState(playerUseCases, toggleFavouriteUseCase, observeFavouriteIdsUseCase, viewModelScope)

    /** null while the first query is running, so the screen can show a loader, not "empty". */
    val songs: StateFlow<List<MusicEntity>?> = observeNeverPlayedUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
