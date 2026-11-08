package com.tasnimulhasan.library

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.metadata.DeleteSongsUseCase
import com.tasnimulhasan.domain.localusecase.metadata.NotifyLibraryChangedUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.localusecase.playlistdetails.InsertMusicToPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlistdetails.IsSongInPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.GetAllPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.InsertPlaylistUseCase
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.metadata.FileOpResult
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import com.tasnimulhasan.ui.component.AddToPlaylistSheet
import com.tasnimulhasan.ui.component.ConfirmDeleteSongsDialog
import com.tasnimulhasan.ui.component.SongActionsSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Long-press actions (play next, queue, add to playlist, info, delete) for the Library song lists. */
@HiltViewModel
class SongActionsViewModel @Inject constructor(
    private val playerUseCases: PlayerUseCases,
    getAllPlaylistUseCase: GetAllPlaylistUseCase,
    private val insertPlaylistUseCase: InsertPlaylistUseCase,
    private val insertMusicToPlaylist: InsertMusicToPlaylistUseCase,
    private val isSongInPlaylistUseCase: IsSongInPlaylistUseCase,
    private val deleteSongsUseCase: DeleteSongsUseCase,
    private val notifyLibraryChangedUseCase: NotifyLibraryChangedUseCase,
) : ViewModel() {

    private val getAllPlaylists = getAllPlaylistUseCase
    val playlists: StateFlow<List<PlaylistEntity>> = getAllPlaylists.invoke()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val _pendingConsent = MutableStateFlow<android.content.IntentSender?>(null)
    val pendingConsent: StateFlow<android.content.IntentSender?> = _pendingConsent.asStateFlow()

    fun playNext(song: MusicEntity) = viewModelScope.launch {
        playerUseCases.playNext(song)
        _messages.send("Playing next")
    }

    fun playLater(song: MusicEntity) = viewModelScope.launch {
        playerUseCases.playLater(song)
        _messages.send("Added to queue")
    }

    fun addToPlaylist(playlistId: Int, music: MusicEntity) {
        viewModelScope.launch { addInternal(playlistId, music) }
    }

    private suspend fun addInternal(playlistId: Int, music: MusicEntity) {
        val alreadyAdded = isSongInPlaylistUseCase(
            IsSongInPlaylistUseCase.Params(playlistId = playlistId, songId = music.songId)
        )
        if (alreadyAdded) {
            _messages.send("Already added to this playlist")
            return
        }
        val details = PlaylistDetailsEntity(
            playlistId = playlistId,
            contentUri = music.contentUri.toString(),
            songId = music.songId,
            cover = music.contentUri.toString(),
            songTitle = music.songTitle,
            artist = music.artist,
            duration = music.duration,
            album = music.album,
            albumId = music.albumId,
        )
        insertMusicToPlaylist(params = InsertMusicToPlaylistUseCase.Params(details))
        _messages.send("Added to playlist")
    }

    fun createPlaylistAndAdd(name: String, song: MusicEntity) {
        viewModelScope.launch {
            val createdAt = System.currentTimeMillis()
            insertPlaylistUseCase(
                InsertPlaylistUseCase.Params(
                    PlaylistEntity(playlistName = name, playlistDescription = "", createdAt = createdAt)
                )
            )
            val created = getAllPlaylists.invoke().first()
                .filter { it.playlistName == name && it.createdAt == createdAt }
                .maxByOrNull { it.id }
            if (created == null) {
                _messages.send("Couldn't create playlist")
                return@launch
            }
            addInternal(created.id, song)
        }
    }

    fun deleteSong(song: MusicEntity) {
        viewModelScope.launch {
            when (val result = deleteSongsUseCase(listOf(song.songId))) {
                FileOpResult.Success -> _messages.send("Song deleted")
                is FileOpResult.NeedsPermission -> _pendingConsent.value = result.intentSender
                is FileOpResult.Error -> _messages.send(result.message)
            }
        }
    }

    fun onDeleteConsentResult(granted: Boolean) {
        _pendingConsent.value = null
        if (!granted) return
        viewModelScope.launch {
            notifyLibraryChangedUseCase()
            _messages.send("Song deleted")
        }
    }
}

/**
 * Hosts the long-press sheet, playlist picker and delete confirmation for a song list and returns
 * the function to call from a row's long-press: `val openActions = rememberLibrarySongActions(...)`.
 */
@Composable
fun rememberLibrarySongActions(
    navigateToSongDetails: (Long) -> Unit,
    viewModel: SongActionsViewModel = hiltViewModel(),
): (MusicEntity) -> Unit {
    val context = LocalContext.current
    var actionsSong by remember { mutableStateOf<MusicEntity?>(null) }
    var playlistSong by remember { mutableStateOf<MusicEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<MusicEntity?>(null) }
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    val consent by viewModel.pendingConsent.collectAsStateWithLifecycle()
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onDeleteConsentResult(result.resultCode == android.app.Activity.RESULT_OK)
    }
    LaunchedEffect(consent) {
        val sender = consent ?: return@LaunchedEffect
        consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
    }

    SongActionsSheet(
        song = actionsSong,
        onPlayNext = { viewModel.playNext(it); actionsSong = null },
        onPlayLater = { viewModel.playLater(it); actionsSong = null },
        onAddToPlaylist = { actionsSong = null; playlistSong = it },
        onSongInfo = { actionsSong = null; navigateToSongDetails(it.songId) },
        onDismiss = { actionsSong = null },
        onDelete = { actionsSong = null; deleteTarget = it },
    )

    deleteTarget?.let { target ->
        ConfirmDeleteSongsDialog(
            songCount = 1,
            firstTitle = target.songTitle,
            onConfirm = { viewModel.deleteSong(target); deleteTarget = null },
            onDismiss = { deleteTarget = null },
        )
    }

    AddToPlaylistSheet(
        song = playlistSong,
        playlists = playlists,
        onPlaylistSelected = { song, playlist -> viewModel.addToPlaylist(playlist.id, song); playlistSong = null },
        onCreatePlaylist = { song, name -> viewModel.createPlaylistAndAdd(name, song); playlistSong = null },
        onDismiss = { playlistSong = null },
    )

    return remember { { song: MusicEntity -> actionsSong = song } }
}
