package com.tasnimulhasan.playlistdetails

import com.tasnimulhasan.ui.motion.entrance

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
import com.tasnimulhasan.ui.reorder.dragItem
import com.tasnimulhasan.ui.reorder.dragReorder
import com.tasnimulhasan.ui.reorder.rememberDragReorderState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import com.tasnimulhasan.designsystem.component.Icon
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
import androidx.compose.runtime.setValue
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
import android.widget.Toast
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import com.tasnimulhasan.ui.component.DetailsHero
import com.tasnimulhasan.ui.component.DetailsSongRow
import com.tasnimulhasan.ui.component.DetailsSort
import com.tasnimulhasan.ui.component.DetailsSortSheet
import com.tasnimulhasan.ui.component.DetailsTopBar
import com.tasnimulhasan.ui.component.HeroCoverTile
import com.tasnimulhasan.ui.component.rememberCoverAccent
import com.tasnimulhasan.ui.component.sortedFor
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

    // Press and hold a song, then drag it up or down to change its place. The list is rearranged
    // here while the finger moves and saved once when it lifts.
    var shown by remember { mutableStateOf<List<PlaylistDetailsEntity>>(emptyList()) }
    // Only the manual order can be dragged; the header is item 0, so list indexes are shifted by one.
    var sort by remember { mutableStateOf(DetailsSort.DEFAULT) }
    var showSort by remember { mutableStateOf(false) }
    val reorder = rememberDragReorderState(
        listState = listState,
        onSwap = { from, to ->
            if (from >= 1 && to >= 1 && from - 1 < shown.size && to - 1 < shown.size) {
                shown = shown.toMutableList().apply { add(to - 1, removeAt(from - 1)) }
            }
        },
        onDrop = { _, _ -> viewModel.action(UiAction.ReorderPlaylist(shown)) },
        canDrag = { key -> key != HERO_KEY && sort == DetailsSort.DEFAULT },
    )
    LaunchedEffect(musicList.value) { if (!reorder.isDragging) shown = musicList.value }

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
    val displayed = remember(shown, sort) {
        shown.sortedFor(sort, { it.songTitle }, { it.artist ?: "" }, { it.duration.toLongOrNull() ?: 0L })
    }
    val musicListForPlay = remember(displayed) {
        displayed.map { item ->
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

    val context = LocalContext.current
    val first = displayed.firstOrNull()
    val accent = rememberCoverAccent(first?.contentUri?.toUri(), first?.albumId ?: 0L)
    val count = musicList.value.size

    fun play(list: List<MusicEntity>) {
        if (list.isEmpty()) return
        viewModel.ensurePlaybackServiceStarted()
        viewModel.playFromPlaylist(list, 0)
        navigateToPlayer(list.first().songId.toString())
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().dragReorder(reorder),
            state = listState,
            userScrollEnabled = !reorder.isDragging,
        ) {
            item(key = HERO_KEY) {
                DetailsHero(
                    title = playlistName.ifBlank { "Playlist" },
                    subtitle = if (count == 1) "1 song" else "$count songs",
                    accent = accent,
                    canPlay = count > 0,
                    onPlay = { play(musicListForPlay) },
                    onShuffle = { play(musicListForPlay.shuffled()) },
                    cover = {
                        if (first != null) HeroCoverTile(first.songId, first.contentUri.toUri(), first.albumId ?: 0L, accent)
                        else Box(Modifier.size(230.dp))
                    },
                )
            }
            if (isEmpty.value) {
                item(key = "empty") {
                    Text(
                        text = "No songs in this playlist yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            } else {
                itemsIndexed(
                    items = displayed,
                    key = { _, item -> item.songId }
                ) { index, item ->
                    DetailsSongRow(
                        modifier = (if (reorder.draggingKey == item.songId) Modifier else Modifier.animateItem())
                            .dragItem(reorder, item.songId)
                            .entrance(index),
                        songId = item.songId,
                        contentUri = item.contentUri.toUri(),
                        albumId = item.albumId ?: 0L,
                        title = item.songTitle,
                        subtitle = listOfNotNull(item.artist?.takeIf { it.isNotBlank() }, item.album?.takeIf { it.isNotBlank() })
                            .joinToString(" - "),
                        selected = (currentSelectedAudio?.songId ?: 0L) == item.songId,
                        onClick = {
                            viewModel.ensurePlaybackServiceStarted()
                            viewModel.playFromPlaylist(musicListForPlay, index)
                            navigateToPlayer(item.songId.toString())
                        },
                        trailing = {
                            val fav = favorites.contains(item.songId)
                            IconButton(onClick = { viewModel.action(UiAction.ToggleFavorite(item.songId)) }) {
                                Icon(
                                    imageVector = if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favourite",
                                    tint = if (fav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { viewModel.action(UiAction.RemoveFromPlaylist(item)) }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove from playlist",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
            }
        }
        DetailsTopBar(
            onBack = onNavigateUp,
            onSort = { showSort = true },
            menuItems = if (count == 0) emptyList() else listOf(
                "Play next" to {
                    viewModel.playNextAll(musicListForPlay)
                    Toast.makeText(context, "Playing next", Toast.LENGTH_SHORT).show()
                },
                "Add to queue" to {
                    viewModel.playLaterAll(musicListForPlay)
                    Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                },
            ),
        )
    }

    if (showSort) {
        DetailsSortSheet(current = sort, onSelect = { sort = it }, onDismiss = { showSort = false })
    }
}

private const val HERO_KEY = "hero"
