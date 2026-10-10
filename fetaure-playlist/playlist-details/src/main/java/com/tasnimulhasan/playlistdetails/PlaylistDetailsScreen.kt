package com.tasnimulhasan.playlistdetails

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.domain.localusecase.playlistdetails.GetAllMusicFromPlaylistUseCase
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import com.tasnimulhasan.playlistdetails.component.MusicCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PlaylistDetailsScreen(
    playlistId: Int,
    playlistName: String,
    onNavigateUp: () -> Unit,
    navigateToPlayer: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistDetailsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val musicList = remember { mutableStateOf<List<PlaylistDetailsEntity>>(emptyList()) }
    val isEmpty = remember { mutableStateOf(false) }
    val snackBarHostState = remember { SnackbarHostState() }
    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.action(UiAction.FetchMusicList(GetAllMusicFromPlaylistUseCase.Params(playlistId)))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    scope.launch {
                        snackBarHostState.showSnackbar(event.message)
                    }
                }
                is UiEvent.Loading -> Unit
                is UiEvent.DataEmpty -> {
                    isEmpty.value = true
                    musicList.value = emptyList()
                }
                is UiEvent.MusicList -> {
                    isEmpty.value = false
                    musicList.value = event.musicList
                }
            }
        }
    }

    // Built once per list emission and handed straight to playCuratedQueue: this is the
    // ENTIRE player queue for this screen, containing only the songs in this playlist - not
    // the full library. Next/Previous can therefore only ever move within these songs.
    val musicListForPlay = remember(musicList.value) {
        musicList.value.map { item ->
            MusicEntity(
                contentUri = item.contentUri.toUri(),
                songId = item.songId,
                cover = null, // not needed for playback, no decode here
                songTitle = item.songTitle,
                artist = item.artist ?: "",
                duration = item.duration,
                album = item.album ?: "",
                albumId = item.albumId ?: 0L
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PlaylistDetailsHeader(
            playlistName = playlistName,
            songCount = musicList.value.size,
            onPlayAll = {
                if (musicListForPlay.isNotEmpty()) {
                    viewModel.ensurePlaybackServiceStarted()
                    viewModel.playFromPlaylist(musicListForPlay, 0)
                    navigateToPlayer(musicListForPlay.first().songId.toString())
                }
            }
        )

        if (!isEmpty.value && musicList.value.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isEmpty.value -> {
                    Text(
                        text = "No songs in this playlist yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState
                    ) {
                        itemsIndexed(
                            items = musicList.value,
                            key = { _, item -> item.songId }
                        ) { index, item ->
                            MusicCard(
                                modifier = Modifier.animateItem(),
                                contentUri = item.contentUri.toUri(),
                                albumId = item.albumId,
                                title = item.songTitle,
                                artist = item.artist ?: "",
                                duration = item.duration,
                                songId = item.songId,
                                selectedId = currentSelectedAudio?.songId ?: 0L,
                                isPlaying = isPlaying,
                                isFavourite = favorites.contains(item.songId),
                                onMusicClicked = {
                                    viewModel.ensurePlaybackServiceStarted()
                                    viewModel.playFromPlaylist(musicListForPlay, index)
                                    navigateToPlayer(item.songId.toString())
                                },
                                onMusicLongClicked = {
                                    viewModel.action(UiAction.RemoveFromPlaylist(item))
                                },
                                onFavouriteIconClicked = {
                                    viewModel.action(UiAction.ToggleFavorite(item.songId))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailsHeader(
    playlistName: String,
    songCount: Int,
    onPlayAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlistName.ifBlank { "Playlist" },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = if (songCount == 1) "1 song" else "$songCount songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (songCount > 0) {
            FilledIconButton(
                onClick = onPlayAll,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Play all")
            }
        }
    }
}
