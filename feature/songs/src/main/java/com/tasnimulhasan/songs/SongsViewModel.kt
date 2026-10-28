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
import com.tasnimulhasan.domain.localusecase.metadata.DeleteSongsUseCase
import com.tasnimulhasan.domain.localusecase.metadata.NotifyLibraryChangedUseCase
import com.tasnimulhasan.entity.metadata.FileOpResult
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
    private val deleteSongsUseCase: DeleteSongsUseCase,
    private val notifyLibraryChangedUseCase: NotifyLibraryChangedUseCase,
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

    /** Adds all [songs] to [playlistId], skipping any already in it, with one summary message. */
    fun addSongsToPlaylist(playlistId: Int, songs: List<MusicEntity>) {
        viewModelScope.launch { addAll(playlistId, songs) }
    }

    /** Creates a playlist called [name] and puts all [songs] into it. */
    fun createPlaylistAndAddAll(name: String, songs: List<MusicEntity>) {
        viewModelScope.launch {
            val createdAt = System.currentTimeMillis()
            insertPlaylistUseCase(
                InsertPlaylistUseCase.Params(
                    PlaylistEntity(playlistName = name, playlistDescription = "", createdAt = createdAt)
                )
            )
            val created = getAllPlaylistUseCase().first()
                .filter { it.playlistName == name && it.createdAt == createdAt }
                .maxByOrNull { it.id }
            if (created == null) {
                _messages.send("Couldn't create playlist")
                return@launch
            }
            addAll(created.id, songs)
        }
    }

    private suspend fun addAll(playlistId: Int, songs: List<MusicEntity>) {
        var added = 0
        var skipped = 0
        songs.forEach { song ->
            val already = isSongInPlaylistUseCase(
                IsSongInPlaylistUseCase.Params(playlistId = playlistId, songId = song.songId)
            )
            if (already) {
                skipped++
            } else {
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
                added++
            }
        }
        _messages.send(
            when {
                added == 0 -> "Already in this playlist"
                skipped == 0 -> if (added == 1) "Added 1 song to playlist" else "Added $added songs to playlist"
                else -> "Added $added songs ($skipped already in playlist)"
            }
        )
    }

    // ---- delete (files are removed from the device) ------------------------------------------

    private val _pendingConsent = MutableStateFlow<android.content.IntentSender?>(null)
    /** Set when Android needs the user's OK to delete; the screen launches it and calls [onConsentResult]. */
    val pendingConsent: StateFlow<android.content.IntentSender?> = _pendingConsent.asStateFlow()
    private var pendingDeleteCount = 0

    fun deleteSongs(songs: List<MusicEntity>) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            val count = songs.size
            when (val result = deleteSongsUseCase(songs.map { it.songId })) {
                FileOpResult.Success -> _messages.send(deletedMessage(count))
                is FileOpResult.NeedsPermission -> {
                    pendingDeleteCount = count
                    _pendingConsent.value = result.intentSender
                }
                is FileOpResult.Error -> _messages.send(result.message)
            }
        }
    }

    /** After the system's delete prompt: approval means Android already deleted the files. */
    fun onConsentResult(granted: Boolean) {
        _pendingConsent.value = null
        val count = pendingDeleteCount
        pendingDeleteCount = 0
        if (!granted) return
        viewModelScope.launch {
            notifyLibraryChangedUseCase()
            _messages.send(deletedMessage(count))
        }
    }

    private fun deletedMessage(count: Int) = if (count == 1) "Song deleted" else "$count songs deleted"

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
