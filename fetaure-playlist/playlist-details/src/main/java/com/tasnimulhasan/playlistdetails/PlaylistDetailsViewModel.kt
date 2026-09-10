package com.tasnimulhasan.playlistdetails

import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.localusecase.playlistdetails.GetAllMusicFromPlaylistUseCase
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    private val getAllMusicFromPlaylistUseCase: GetAllMusicFromPlaylistUseCase,
    private val playerUseCases: PlayerUseCases,
) : BaseViewModel() {

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent get() = _uiEvent.receiveAsFlow()

    val action: (UiAction) -> Unit = {
        when (it) {
            is UiAction.FetchMusicList -> getMusicList(it.params)
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

    fun setMediaItems(musicList: List<MusicEntity>, sortType: SortType) {
        viewModelScope.launch {
            playerUseCases.loadPlaylist(musicList, sortType)
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
}