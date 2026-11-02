package com.tasnimulhasan.albums

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.designsystem.component.Icon
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.ui.component.DetailsHero
import com.tasnimulhasan.ui.component.DetailsSongRow
import com.tasnimulhasan.ui.component.DetailsSort
import com.tasnimulhasan.ui.component.DetailsSortSheet
import com.tasnimulhasan.ui.component.DetailsTopBar
import com.tasnimulhasan.ui.component.HeroCoverCircle
import com.tasnimulhasan.ui.component.rememberCoverAccent
import com.tasnimulhasan.ui.component.sortedFor
import com.tasnimulhasan.ui.motion.entrance

@Composable
internal fun AlbumDetailsScreen(
    modifier: Modifier = Modifier,
    navigateToPlayer: (musicId: String) -> Unit,
    navigateBack: () -> Unit = {},
    viewModel: AlbumDetailsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    AlbumDetailsList(
        modifier = modifier,
        songs = songs,
        selectedId = currentSelectedAudio.songId,
        isPlaying = isPlaying,
        favorites = favorites,
        onSongClicked = { songId ->
            viewModel.ensurePlaybackServiceStarted()
            if (currentSelectedAudio.songId != songId) viewModel.playSong(songId)
            navigateToPlayer(songId.toString())
        },
        onFavouriteClicked = viewModel::toggleFavorite,
        onBack = navigateBack,
        onPlayNext = { list ->
            viewModel.playNextAll(list)
            Toast.makeText(context, "Playing next", Toast.LENGTH_SHORT).show()
        },
        onPlayLater = { list ->
            viewModel.playLaterAll(list)
            Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
        },
    )
}

@Composable
internal fun AlbumDetailsList(
    modifier: Modifier = Modifier,
    songs: List<MusicEntity>,
    selectedId: Long,
    isPlaying: Boolean,
    favorites: Set<Long>,
    onSongClicked: (Long) -> Unit,
    onFavouriteClicked: (Long) -> Unit,
    onBack: () -> Unit = {},
    onPlayNext: (List<MusicEntity>) -> Unit = {},
    onPlayLater: (List<MusicEntity>) -> Unit = {},
) {
    if (songs.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var sort by remember { mutableStateOf(DetailsSort.DEFAULT) }
    var showSort by remember { mutableStateOf(false) }
    val shown = remember(songs, sort) {
        songs.sortedFor(sort, { it.songTitle }, { it.artist }, { it.duration.toLongOrNull() ?: 0L })
    }
    val first = songs.first()
    val accent = rememberCoverAccent(first.contentUri, first.albumId)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "hero") {
                DetailsHero(
                    title = first.album.ifBlank { "Album" },
                    subtitle = if (songs.size == 1) "1 song" else "${songs.size} songs",
                    accent = accent,
                    canPlay = true,
                    onPlay = { onSongClicked(shown.first().songId) },
                    onShuffle = { onSongClicked(songs.random().songId) },
                    cover = { HeroCoverCircle(first.songId, first.contentUri, first.albumId) },
                )
            }
            itemsIndexed(items = shown, key = { _, item -> item.songId }) { index, item ->
                DetailsSongRow(
                    modifier = Modifier.animateItem().entrance(index),
                    songId = item.songId,
                    contentUri = item.contentUri,
                    albumId = item.albumId,
                    title = item.songTitle,
                    subtitle = if (item.album.isBlank()) item.artist else "${item.artist} - ${item.album}",
                    selected = selectedId == item.songId,
                    onClick = { onSongClicked(item.songId) },
                    trailing = {
                        val fav = favorites.contains(item.songId)
                        IconButton(onClick = { onFavouriteClicked(item.songId) }) {
                            Icon(
                                imageVector = if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favourite",
                                tint = if (fav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }
        }
        DetailsTopBar(
            onBack = onBack,
            onSort = { showSort = true },
            menuItems = listOf(
                "Play next" to { onPlayNext(shown) },
                "Add to queue" to { onPlayLater(shown) },
            ),
        )
    }

    if (showSort) {
        DetailsSortSheet(current = sort, onSelect = { sort = it }, onDismiss = { showSort = false })
    }
}
