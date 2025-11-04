package com.tasnimulhasan.playlists

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import com.tasnimulhasan.domain.localusecase.playlists.InsertPlaylistUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

@Composable
internal fun PlaylistsRoute(
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val playlists = remember { mutableStateOf<List<PlaylistEntity>>(emptyList()) }
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
        snackBarHostState = snackBarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    playlists: List<PlaylistEntity>,
    isEmpty: Boolean,
    onCreatePlaylist: (String, String) -> Unit,
    snackBarHostState: SnackbarHostState
) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Add Playlist"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isEmpty -> {
                    Text(
                        text = "No Playlists Found",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                playlists.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        items(playlists) { playlist ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = playlist.playlistName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Created: ${SimpleDateFormat("yyyy-MM-dd HH:mm").format(playlist.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Dialog")
                }

                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Create Playlist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Playlist Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onCreate(name, description)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Playlist")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistsScreenPreview() {
    PlaylistsScreen(
        playlists = listOf(
            PlaylistEntity(1, "Chill Vibes", "Relaxing tunes", System.currentTimeMillis())
        ),
        isEmpty = false,
        onCreatePlaylist = { _, _ -> },
        snackBarHostState = remember { SnackbarHostState() }
    )
}