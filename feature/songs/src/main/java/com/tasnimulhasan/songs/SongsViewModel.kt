package com.tasnimulhasan.songs

import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.datastore.GetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ToggleFavouriteUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.localusecase.playlistdetails.InsertMusicToPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlistdetails.IsSongInPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.GetAllPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.InsertPlaylistUseCase
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val getAllPlaylistUseCase: GetAllPlaylistUseCase,
    private val insertPlaylistUseCase: InsertPlaylistUseCase,
    private val insertMusicToPlaylist: InsertMusicToPlaylistUseCase,
    private val isSongInPlaylistUseCase: IsSongInPlaylistUseCase,
) : BaseViewModel() {

    private val _messages = Channel<String>(Channel.BUFFERED)
    /** One-shot user messages ("Added to queue", ...) shown as a toast by the screen. */
    val messages = _messages.receiveAsFlow()

    val playlists: StateFlow<List<PlaylistEntity>> = getAllPlaylistUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun playNext(song: MusicEntity) = viewModelScope.launch {
        playerUseCases.playNext(song)
        _messages.send("Playing next")
    }

    fun playLater(song: MusicEntity) = viewModelScope.launch {
        playerUseCases.playLater(song)
        _messages.send("Added to queue")
    }

    fun addToPlaylist(playlistId: Int, song: MusicEntity) {
        viewModelScope.launch { addSongToPlaylist(playlistId, song) }
    }

    /** Creates a playlist called [name] and puts [song] straight into it. */
    fun createPlaylistAndAdd(name: String, song: MusicEntity) {
        viewModelScope.launch {
            val createdAt = System.currentTimeMillis()
            insertPlaylistUseCase(
                InsertPlaylistUseCase.Params(
                    PlaylistEntity(playlistName = name, playlistDescription = "", createdAt = createdAt)
                )
            )
            // insertPlaylist returns nothing, so look the new row up (newest with this name).
            val created = getAllPlaylistUseCase().first()
                .filter { it.playlistName == name && it.createdAt == createdAt }
                .maxByOrNull { it.id }
            if (created == null) {
                _messages.send("Couldn't create playlist")
                return@launch
            }
            addSongToPlaylist(created.id, song)
        }
    }

    private suspend fun addSongToPlaylist(playlistId: Int, song: MusicEntity) {
        val alreadyAdded = isSongInPlaylistUseCase(
            IsSongInPlaylistUseCase.Params(playlistId = playlistId, songId = song.songId)
        )
        if (alreadyAdded) {
            _messages.send("Already added to this playlist")
            return
        }
        insertMusicToPlaylist(
            params = InsertMusicToPlaylistUseCase.Params(
                PlaylistDetailsEntity(
                    playlistId = playlistId,
                    contentUri = song.contentUri.toString(),
                    songId = song.songId,
                    cover = song.contentUri.toString(),
                    songTitle = song.songTitle,
                    artist = song.artist,
                    duration = song.duration,
                    album = song.album,
                    albumId = song.albumId,
                )
            )
        )
        _messages.send("Added to playlist")
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
