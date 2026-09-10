package com.tasnimulhasan.home

import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.datastore.GetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.music.FetchMusicUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.localusecase.playlistdetails.InsertMusicListToPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlistdetails.InsertMusicToPlaylistUseCase
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fetchMusicUseCase: FetchMusicUseCase,
    private val playerUseCases: PlayerUseCases,
    private val setSortTypeUseCase: SetSortTypeUseCase,
    private val getSortTypeUseCase: GetSortTypeUseCase,
    private val getAllPlaylistUseCase: GetAllPlaylistUseCase,
    private val insertMusicToPlaylist: InsertMusicToPlaylistUseCase,
    private val insertMusicListToPlaylistUseCase: InsertMusicListToPlaylistUseCase,
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

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentSelectedAudio = MutableStateFlow(dummyAudio)
    val currentSelectedAudio = _currentSelectedAudio.asStateFlow()

    private val _audioList = MutableStateFlow<List<MusicEntity>>(emptyList())
    val audioList: StateFlow<List<MusicEntity>> = _audioList.asStateFlow()

    private val _uIState: MutableStateFlow<UIState> = MutableStateFlow(UIState.Initial)
    val uIState: StateFlow<UIState> = _uIState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent get() = _uiEvent.receiveAsFlow()

    private val _favorites = MutableStateFlow<Set<Long>>(emptySet())
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

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

                val sorted = fetchMusicUseCase(persistedSortType)
                _audioList.value = sorted
                _uIState.value = UIState.MusicList(sorted)

                playerUseCases.loadPlaylist(sorted, persistedSortType)
                restorePlaybackState()
            }
        }

        viewModelScope.launch {
            playerUseCases.observeAudioState().collectLatest { mediaState ->
                when (mediaState) {
                    PlaybackState.Idle -> _uIState.value = UIState.Initial
                    is PlaybackState.Buffering -> calculateProgressValue(mediaState.position)
                    is PlaybackState.Playing -> _isPlaying.value = mediaState.isPlaying
                    is PlaybackState.Progress -> calculateProgressValue(mediaState.position)
                    is PlaybackState.TrackChanged -> {
                        _currentSelectedAudio.value = _audioList.value.getOrNull(mediaState.index) ?: dummyAudio
                    }
                    is PlaybackState.Ready -> {
                        _duration.value = mediaState.duration
                        _uIState.value = UIState.Ready
                        calculateProgressValue(playerUseCases.getPlaybackSnapshot().position)
                    }
                }
            }
        }
    }

    private suspend fun restorePlaybackState() {
        val snapshot = playerUseCases.getPlaybackSnapshot()
        _currentSelectedAudio.value = _audioList.value.getOrNull(snapshot.currentIndex) ?: dummyAudio
        _duration.value = snapshot.duration
        calculateProgressValue(snapshot.position)
        _isPlaying.value = snapshot.isPlaying
    }

    fun setSortType(type: SortType) {
        viewModelScope.launch {
            _sortType.value = type
            setSortTypeUseCase(type)
            val sortedList = fetchMusicUseCase(type)
            _audioList.value = sortedList
            _uIState.value = UIState.MusicList(sortedList)
            playerUseCases.loadPlaylist(sortedList, type)
            initializedList.value = true
        }
    }

    fun onUiEvents(uiEvents: UIEvents) = viewModelScope.launch {
        when (uiEvents) {
            is UIEvents.Backward -> playerUseCases.backwardTrackUseCase()
            is UIEvents.Forward -> playerUseCases.forwardTrackUseCase()
            is UIEvents.PlayPause -> {
                if (_isPlaying.value) playerUseCases.pause() else playerUseCases.play()
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

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            val current = _favorites.value
            _favorites.value = if (current.contains(songId)) current - songId else current + songId
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