package com.tasnimulhasan.playlistdetails

import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ToggleFavouriteUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.localusecase.playlistdetails.DeleteMusicFromPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlistdetails.GetAllMusicFromPlaylistUseCase
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    private val getAllMusicFromPlaylistUseCase: GetAllMusicFromPlaylistUseCase,
    private val deleteMusicFromPlaylistUseCase: DeleteMusicFromPlaylistUseCase,
    private val insertMusicListToPlaylistUseCase: com.tasnimulhasan.domain.localusecase.playlistdetails.InsertMusicListToPlaylistUseCase,
    private val playerUseCases: PlayerUseCases,
    private val observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
) : BaseViewModel() {

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent get() = _uiEvent.receiveAsFlow()

    // Same shared state the mini player / full player / home list read, so this screen's
    // "now playing" row highlight can never point at a different song than what's actually
    // playing.
    val currentSelectedAudio: StateFlow<MusicEntity?> = playerUseCases.observeCurrentSelectedAudio()
    val isPlaying: StateFlow<Boolean> = playerUseCases.observeIsPlaying()

    val favorites: StateFlow<Set<Long>> = observeFavouriteIdsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val action: (UiAction) -> Unit = {
        when (it) {
            is UiAction.FetchMusicList -> getMusicList(it.params)
            is UiAction.RemoveFromPlaylist -> removeFromPlaylist(it.item)
            is UiAction.ToggleFavorite -> toggleFavorite(it.songId)
            is UiAction.ReorderPlaylist -> reorderPlaylist(it.itemsInNewOrder)
        }
    }

    private fun getMusicList(params: GetAllMusicFromPlaylistUseCase.Params) {
        execute {
            _uiEvent.send(UiEvent.Loading(true))
            getAllMusicFromPlaylistUseCase.invoke(params = params).collect {
                _uiEvent.send(UiEvent.Loading(false))
                if (it.isEmpty()) _uiEvent.send(UiEvent.DataEmpty)
                else _uiEvent.send(UiEvent.MusicList(it))
            }
        }
    }

    private fun removeFromPlaylist(item: PlaylistDetailsEntity) {
        execute {
            deleteMusicFromPlaylistUseCase.invoke(DeleteMusicFromPlaylistUseCase.Params(item))
            _uiEvent.send(UiEvent.ShowToast("Removed from playlist"))
        }
    }

    /**
     * Saves a new order. A playlist is shown newest-id-first, so the new order is stored by handing
     * the SAME set of ids back out in that order: each row takes over an id from the old arrangement.
     * Every id is reused exactly once, so the REPLACE insert (one transaction) leaves no duplicates
     * or gaps, and no schema change is needed.
     */
    private fun reorderPlaylist(itemsInNewOrder: List<PlaylistDetailsEntity>) {
        if (itemsInNewOrder.size < 2) return
        val ids = itemsInNewOrder.map { it.id }.sortedDescending()
        val reassigned = itemsInNewOrder.mapIndexed { position, item -> item.copy(id = ids[position]) }
        execute {
            insertMusicListToPlaylistUseCase.invoke(
                com.tasnimulhasan.domain.localusecase.playlistdetails.InsertMusicListToPlaylistUseCase.Params(reassigned)
            )
        }
    }

    private fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            toggleFavouriteUseCase(songId)
        }
    }

    fun playFromPlaylist(musicList: List<MusicEntity>, startIndex: Int) {
        viewModelScope.launch {
            playerUseCases.playCuratedQueue(musicList, startIndex)
        }
    }

    fun isPlaybackServiceRunning(): Boolean = playerUseCases.isPlaybackServiceRunning()
    fun ensurePlaybackServiceStarted() = playerUseCases.ensurePlaybackServiceStarted()
}

sealed interface UiEvent {
    data class Loading(val loading: Boolean) : UiEvent
    data class ShowToast(val message: String) : UiEvent
    data object DataEmpty : UiEvent
    data class MusicList(val musicList: List<PlaylistDetailsEntity>) : UiEvent
}

sealed interface UiAction {
    data class FetchMusicList(val params: GetAllMusicFromPlaylistUseCase.Params) : UiAction
    data class RemoveFromPlaylist(val item: PlaylistDetailsEntity) : UiAction
    data class ToggleFavorite(val songId: Long) : UiAction
    data class ReorderPlaylist(val itemsInNewOrder: List<PlaylistDetailsEntity>) : UiAction
}
