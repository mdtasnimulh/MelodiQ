package com.tasnimulhasan.playlists

import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.playlists.DeleteAllPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.DeletePlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.GetAllPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.InsertPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.SearchPlaylistByNameUseCase
import com.tasnimulhasan.domain.localusecase.playlists.UpdatePlaylistUseCase
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
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
    private val getAllPlaylistUseCase: GetAllPlaylistUseCase,
    private val searchPlaylistByNameUseCase: SearchPlaylistByNameUseCase
) : BaseViewModel() {

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent get() = _uiEvent.receiveAsFlow()

    val action:(UiAction) -> Unit = {
        when (it) {
            is UiAction.InsertPlaylist -> insertPlaylist(it.params)
            is UiAction.UpdatePlaylist -> {}
            is UiAction.DeletePlaylist -> {}
            is UiAction.SearchPlaylists -> {}
            is UiAction.FetchAllPlaylists -> {}
        }
    }

    private fun insertPlaylist(params: InsertPlaylistUseCase.Params) {
        execute {
            insertPlaylistUseCase.invoke(params = params)
            _uiEvent.send(UiEvent.ShowToast("Playlist Create Successfully"))
        }
    }

    private fun updatePlaylist(params: UpdatePlaylistUseCase.Params) {
        execute {
            updatePlaylistUseCase.invoke(params = params)
            _uiEvent.send(UiEvent.ShowToast("Playlist Updated Successfully"))
        }
    }

    private fun deletePlaylist(params: DeletePlaylistUseCase.Params) {
        execute {
            deletePlaylistUseCase.invoke(params = params)
            _uiEvent.send(UiEvent.ShowToast("Playlist Deleted Successfully"))
        }
    }

    private fun deleteAllPlaylists() {
        execute {
            deleteAllPlaylistUseCase.invoke()
            _uiEvent.send(UiEvent.ShowToast("All Playlists Removed!"))
        }
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

    private fun searchPlaylists(params: SearchPlaylistByNameUseCase.Params) {
        execute {
            _uiEvent.send(UiEvent.Loading(true))
            searchPlaylistByNameUseCase.invoke(params = params).collect {
                _uiEvent.send(UiEvent.Loading(false))
                if (it.isEmpty()) _uiEvent.send(UiEvent.DataEmpty)
                else _uiEvent.send(UiEvent.Playlists(it))
            }
        }
    }

}

sealed interface UiEvent {
    data class Loading(val loading: Boolean) : UiEvent
    data class ShowToast(val message: String) : UiEvent
    data object DataEmpty : UiEvent
    data class Playlists(val playlists: List<PlaylistEntity>) : UiEvent
}

sealed interface UiAction {
    data class InsertPlaylist(val params: InsertPlaylistUseCase.Params) : UiAction
    data class UpdatePlaylist(val params: UpdatePlaylistUseCase.Params) : UiAction
    data class DeletePlaylist(val params: DeletePlaylistUseCase.Params) : UiAction
    data class SearchPlaylists(val params: SearchPlaylistByNameUseCase.Params) : UiAction
    data object FetchAllPlaylists : UiAction
}