package com.tasnimulhasan.home

import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.designsystem.theme.BlueDarker
import com.tasnimulhasan.designsystem.theme.RobotoFontFamily
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import com.tasnimulhasan.home.components.AddToPlaylistDialog
import com.tasnimulhasan.home.components.MusicCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.HomeScreen(
    navigateToPlayer: (String) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val audioList by viewModel.audioList.collectAsStateWithLifecycle()
    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val selectedSortOption = remember { mutableStateOf(viewModel.sortTypeToDisplayString(viewModel.sortType.value)) }
    val showSortDialog = remember { mutableStateOf(false) }

    // NEW STATE FOR THE DIALOG
    val showAddToPlaylistDialog = remember { mutableStateOf(false) }
    val showSongActionsSheet = remember { mutableStateOf(false) }
    val selectedSongForPlaylist = remember { mutableStateOf<MusicEntity?>(null) }

    val snackBarHostState = remember { SnackbarHostState() }
    val playlists = remember { mutableStateOf<List<PlaylistEntity>>(emptyList()) }
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isLoading = remember { mutableStateOf(false) }
    val isEmpty = remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

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

    LaunchedEffect(sortType) {
        selectedSortOption.value = viewModel.sortTypeToDisplayString(sortType)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = modifier
                            .padding(top = 6.dp),
                        text = "Sort Type",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Normal,
                            fontFamily = RobotoFontFamily
                        )
                    )

                    IconButton(
                        onClick = {
                            showSortDialog.value = !showSortDialog.value
                        },
                        modifier = Modifier,
                        enabled = true
                    ) {
                        Icon(
                            modifier = Modifier,
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort Icon",
                            tint = Color.Red
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.playAll() },
                        modifier = Modifier.weight(1f),
                        enabled = audioList.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Play all")
                    }
                    OutlinedButton(
                        onClick = { viewModel.shuffleAll() },
                        modifier = Modifier.weight(1f),
                        enabled = audioList.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Shuffle")
                    }
                }
            }
            itemsIndexed(
                items = audioList,
                key = { _, item -> item.songId }
            ) { index, item ->
                MusicCard(
                    modifier = modifier,
                    path = item.contentUri,
                    title = item.songTitle,
                    artist = item.artist,
                    album = item.album,
                    albumId = item.albumId,
                    duration = item.duration,
                    songId = item.songId,
                    selectedId = currentSelectedAudio.songId,
                    isPlaying = isPlaying,
                    isFavourite = favorites.contains(item.songId),
                    onMusicClicked = {
                        viewModel.ensurePlaybackServiceStarted()
                        if (currentSelectedAudio.songId != item.songId) {
                            viewModel.onUiEvents(UIEvents.SelectedAudioChange(index))
                        }
                        navigateToPlayer(item.songId.toString())
                    },
                    onMusicLongClicked = {
                        selectedSongForPlaylist.value = item
                        showSongActionsSheet.value = true
                    },
                    onFavouriteIconClicked = {
                        viewModel.action(UiAction.ToggleFavorite(item.songId))
                    },
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        }

        SortTypeDialog(
            showDialog = showSortDialog,
            selectedSortOption = selectedSortOption,
            onSortOptionSelected = { selected ->
                Timber.e("SortOption, Selected: $selected")

                val sortedBy = when (selected) {
                    "Date Modified (ASC)" -> SortType.DATE_MODIFIED_ASC
                    "Date Modified (DESC)" -> SortType.DATE_MODIFIED_DESC
                    "Name (ASC)" -> SortType.NAME_ASC
                    "Name (DESC)" -> SortType.NAME_DESC
                    "Artist (ASC)" -> SortType.ARTIST_ASC
                    "Artist (DESC)" -> SortType.ARTIST_DESC
                    "Duration (ASC)" -> SortType.DURATION_ASC
                    "Duration (DESC)" -> SortType.DURATION_DESC
                    else -> SortType.DATE_MODIFIED_DESC
                }

                viewModel.setSortType(sortedBy)
            }
        )

        // NEW DIALOG
        AddToPlaylistDialog(
            show = showAddToPlaylistDialog,
            song = selectedSongForPlaylist.value,
            playlists = playlists.value,               // you already fetch them
            onPlaylistSelected = { playlist ->
                selectedSongForPlaylist.value?.let { song ->
                    viewModel.action(
                        UiAction.AddMusicToPlaylist(
                            playlistId = playlist.id,
                            music = song
                        )
                    )
                }
                showAddToPlaylistDialog.value = false
            },
            onDismiss = { showAddToPlaylistDialog.value = false }
        )

        SongActionsSheet(
            show = showSongActionsSheet,
            song = selectedSongForPlaylist.value,
            onPlayNext = { song ->
                viewModel.playNext(song)
                showSongActionsSheet.value = false
            },
            onPlayLater = { song ->
                viewModel.playLater(song)
                showSongActionsSheet.value = false
            },
            onAddToPlaylist = {
                showSongActionsSheet.value = false
                showAddToPlaylistDialog.value = true
            },
            onDismiss = { showSongActionsSheet.value = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongActionsSheet(
    show: MutableState<Boolean>,
    song: MusicEntity?,
    onPlayNext: (MusicEntity) -> Unit,
    onPlayLater: (MusicEntity) -> Unit,
    onAddToPlaylist: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show.value || song == null) return

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = song.songTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            SheetAction(Icons.Filled.QueuePlayNext, "Play next") { onPlayNext(song) }
            SheetAction(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue") { onPlayLater(song) }
            SheetAction(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist") { onAddToPlaylist() }
        }
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SortTypeDialog(
    showDialog: MutableState<Boolean>,
    selectedSortOption: MutableState<String>,
    onSortOptionSelected: (String) -> Unit
) {
    val options = listOf(
        "Date Modified (ASC)",
        "Date Modified (DESC)",
        "Name (ASC)",
        "Name (DESC)",
        "Artist (ASC)",
        "Artist (DESC)",
        "Duration (ASC)",
        "Duration (DESC)"
    )

    if (showDialog.value) {
        Dialog(onDismissRequest = {
            showDialog.value = false
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(15.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Sort By",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    options.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedSortOption.value = option
                                    onSortOptionSelected(selectedSortOption.value)
                                    showDialog.value = false
                                }
                        ) {
                            Checkbox(
                                checked = selectedSortOption.value == option,
                                onCheckedChange = {
                                    selectedSortOption.value = option
                                    onSortOptionSelected(selectedSortOption.value)
                                    showDialog.value = false
                                }
                            )
                            Text(text = option)
                        }
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
fun <T> Context.isServiceRunning(service: Class<T>): Boolean {
    return (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any { it -> it.service.className == service.name }
}