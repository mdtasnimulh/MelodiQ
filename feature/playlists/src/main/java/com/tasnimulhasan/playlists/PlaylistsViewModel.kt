package com.tasnimulhasan.playlists

import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.playlists.DeleteAllPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.DeletePlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.GetAllPlaylistsWithStatsUseCase
import com.tasnimulhasan.domain.localusecase.playlists.InsertPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.SearchPlaylistByNameUseCase
import com.tasnimulhasan.domain.localusecase.playlists.UpdatePlaylistUseCase
import com.tasnimulhasan.entity.room.playlist.PlaylistWithStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val insertPlaylistUseCase: InsertPlaylistUseCase,
    private val updatePlaylistUseCase: UpdatePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val deleteAllPlaylistUseCase: DeleteAllPlaylistUseCase,
    private val getAllPlaylistsWithStatsUseCase: GetAllPlaylistsWithStatsUseCase,
    private val searchPlaylistByNameUseCase: SearchPlaylistByNameUseCase
) : BaseViewModel() {

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent get() = _uiEvent.receiveAsFlow()

    val action:(UiAction) -> Unit = {
        when (it) {
            is UiAction.InsertPlaylist -> insertPlaylist(it.params)
            is UiAction.UpdatePlaylist -> updatePlaylist(it.params)
            is UiAction.DeletePlaylist -> deletePlaylist(it.params)
            is UiAction.SearchPlaylists -> searchPlaylists(it.params)
            is UiAction.FetchAllPlaylists -> fetchAllPlaylists()
        }
    }

    private fun insertPlaylist(params: InsertPlaylistUseCase.Params) {
        execute {
            insertPlaylistUseCase.invoke(params = params)
            _uiEvent.send(UiEvent.ShowToast("Playlist created"))
        }
    }

    private fun updatePlaylist(params: UpdatePlaylistUseCase.Params) {
        execute {
            updatePlaylistUseCase.invoke(params = params)
            _uiEvent.send(UiEvent.ShowToast("Playlist updated"))
        }
    }

    private fun deletePlaylist(params: DeletePlaylistUseCase.Params) {
        execute {
            deletePlaylistUseCase.invoke(params = params)
            _uiEvent.send(UiEvent.ShowToast("Playlist deleted"))
        }
    }

    private fun deleteAllPlaylists() {
        execute {
            deleteAllPlaylistUseCase.invoke()
            _uiEvent.send(UiEvent.ShowToast("All playlists removed"))
        }
    }

    private fun fetchAllPlaylists() {
        execute {
            _uiEvent.send(UiEvent.Loading(true))
            // Live query: song count/duration per playlist update by themselves whenever
            // playlist_details_table changes - no manual refresh needed after adding/removing
            // a song.
            getAllPlaylistsWithStatsUseCase.invoke().collect {
                _uiEvent.send(UiEvent.Loading(false))
                if (it.isEmpty()) _uiEvent.send(UiEvent.DataEmpty)
                else _uiEvent.send(UiEvent.Playlists(it))
            }
        }
    }

    private fun searchPlaylists(params: SearchPlaylistByNameUseCase.Params) {
        execute {
            _uiEvent.send(UiEvent.Loading(true))
            searchPlaylistByNameUseCase.invoke(params = params).collect {
                _uiEvent.send(UiEvent.Loading(false))
            }
        }
    }

}

sealed interface UiEvent {
    data class Loading(val loading: Boolean) : UiEvent
    data class ShowToast(val message: String) : UiEvent
    data object DataEmpty : UiEvent
    data class Playlists(val playlists: List<PlaylistWithStats>) : UiEvent
}

sealed interface UiAction {
    data class InsertPlaylist(val params: InsertPlaylistUseCase.Params) : UiAction
    data class UpdatePlaylist(val params: UpdatePlaylistUseCase.Params) : UiAction
    data class DeletePlaylist(val params: DeletePlaylistUseCase.Params) : UiAction
    data class SearchPlaylists(val params: SearchPlaylistByNameUseCase.Params) : UiAction
    data object FetchAllPlaylists : UiAction
}
