package com.tasnimulhasan.playlists

import com.tasnimulhasan.ui.motion.entrance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import com.tasnimulhasan.designsystem.component.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tasnimulhasan.domain.localusecase.playlists.DeletePlaylistUseCase
import com.tasnimulhasan.domain.localusecase.playlists.InsertPlaylistUseCase
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistWithStats
import com.tasnimulhasan.playlists.component.CreatePlaylistDialog
import com.tasnimulhasan.playlists.component.PlaylistCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
internal fun PlaylistsRoute(
    onPlaylistClicked: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val playlists = remember { mutableStateOf<List<PlaylistWithStats>>(emptyList()) }
    val isLoading = remember { mutableStateOf(false) }
    val isEmpty = remember { mutableStateOf(false) }
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.action(UiAction.FetchAllPlaylists)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    scope.launch {
                        snackBarHostState.showSnackbar(event.message)
                    }
                }
                is UiEvent.Loading -> isLoading.value = event.loading
                is UiEvent.DataEmpty -> {
                    isEmpty.value = true
                    playlists.value = emptyList()
                }
                is UiEvent.Playlists -> {
                    isEmpty.value = false
                    playlists.value = event.playlists
                }
            }
        }
    }

    PlaylistsScreen(
        modifier = modifier,
        playlists = playlists.value,
        isEmpty = isEmpty.value,
        onCreatePlaylist = { name, desc ->
            viewModel.action(
                UiAction.InsertPlaylist(
                    InsertPlaylistUseCase.Params(
                        PlaylistEntity(
                            playlistName = name,
                            playlistDescription = desc,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                )
            )
        },
        onDeletePlaylist = { playlist ->
            viewModel.action(UiAction.DeletePlaylist(DeletePlaylistUseCase.Params(playlist)))
        },
        snackBarHostState = snackBarHostState,
        onPlaylistClicked = { id, name -> onPlaylistClicked.invoke(id, name) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    playlists: List<PlaylistWithStats>,
    isEmpty: Boolean,
    onCreatePlaylist: (String, String) -> Unit,
    onPlaylistClicked: (Int, String) -> Unit,
    snackBarHostState: SnackbarHostState,
    onDeletePlaylist: (PlaylistEntity) -> Unit = {},
) {
    var showDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }

    // The create button used to be a FloatingActionButton, which the mini/pop-up player
    // could sit on top of, making it unreachable while music was playing. It now lives in
    // this header row instead, so it's never covered by anything anchored to the bottom of
    // the screen.
    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .entrance(0)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Your Playlists",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FilledIconButton(
                    onClick = { showDialog = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Create playlist"
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isEmpty -> EmptyPlaylistsState(onCreateClicked = { showDialog = true })

                    playlists.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp + com.tasnimulhasan.designsystem.component.LocalFloatingBarPadding.current),
                        ) {
                            itemsIndexed(playlists, key = { _, it -> it.playlist.id }) { index, playlist ->
                                PlaylistCard(
                                    modifier = Modifier.animateItem().entrance(index),
                                    playlist = playlist,
                                    onPlaylistClicked = onPlaylistClicked,
                                    onDeleteClicked = { playlistToDelete = playlist.playlist },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    playlistToDelete?.let { target ->
        com.tasnimulhasan.ui.component.MelodiqConfirmSheet(
            title = "Delete playlist?",
            message = "\"${target.playlistName}\" will be deleted. The songs on your phone are not touched.",
            confirmLabel = "Delete",
            destructive = true,
            icon = Icons.Filled.Delete,
            onConfirm = { onDeletePlaylist(target) },
            onDismiss = { playlistToDelete = null },
        )
    }

    if (showDialog) {
        CreatePlaylistDialog(
            onDismiss = { showDialog = false },
            onCreate = { name, desc ->
                onCreatePlaylist(name, desc)
                showDialog = false
            }
        )
    }
}

@Composable
private fun EmptyPlaylistsState(onCreateClicked: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp),
            )
        }
        Text(
            text = "No playlists yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Tap + to create your first playlist",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistsScreenPreview() {
    PlaylistsScreen(
        playlists = listOf(
            PlaylistWithStats(
                PlaylistEntity(1, "Chill Vibes", "Relaxing tunes", System.currentTimeMillis()),
                songCount = 5,
                totalDurationMs = 720_000L,
            )
        ),
        isEmpty = false,
        onCreatePlaylist = { _, _ -> },
        snackBarHostState = remember { SnackbarHostState() },
        onPlaylistClicked = { _, _ -> }
    )
}
