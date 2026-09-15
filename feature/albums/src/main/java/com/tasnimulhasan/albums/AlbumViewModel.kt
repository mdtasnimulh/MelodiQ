package com.tasnimulhasan.albums

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AlbumUiModel(
    val albumId: Long,
    val albumName: String,
    val artist: String,
    val songCount: Int,
    val representativeSongId: Long,
    val representativeContentUri: Uri,
)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val playerUseCases: PlayerUseCases,
) : BaseViewModel() {

    // Derived straight from the shared, already-cached song list - no separate MediaStore
    // query needed, and it can never drift out of sync with what the rest of the app shows.
    val albums: StateFlow<List<AlbumUiModel>> = playerUseCases.observeAudioList()
        .map { songs ->
            songs.groupBy { it.albumId }
                .map { (albumId, songsInAlbum) ->
                    val first = songsInAlbum.first()
                    AlbumUiModel(
                        albumId = albumId,
                        albumName = first.album.ifBlank { "Unknown Album" },
                        artist = first.artist,
                        songCount = songsInAlbum.size,
                        representativeSongId = first.songId,
                        representativeContentUri = first.contentUri,
                    )
                }
                .sortedBy { it.albumName.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
