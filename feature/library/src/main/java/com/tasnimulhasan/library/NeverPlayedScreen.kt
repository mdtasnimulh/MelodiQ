package com.tasnimulhasan.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.library.components.LibrarySongRow

@Composable
internal fun NeverPlayedRoute(
    modifier: Modifier = Modifier,
    navigateToPlayer: (musicId: String) -> Unit,
    viewModel: NeverPlayedViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val current by viewModel.playback.currentSelectedAudio.collectAsStateWithLifecycle()
    val favorites by viewModel.playback.favorites.collectAsStateWithLifecycle()

    val list = songs
    when {
        list == null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        list.isEmpty() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "You've played every song in your library",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )
        }
        else -> LazyColumn(modifier = modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${list.size} songs you haven't played yet",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        viewModel.playback.playList(list.shuffled())
                        navigateToPlayer(list.first().songId.toString())
                    }) { Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle all") }
                    IconButton(onClick = {
                        viewModel.playback.playList(list)
                        navigateToPlayer(list.first().songId.toString())
                    }) { Icon(Icons.Filled.PlayCircle, contentDescription = "Play all") }
                }
            }
            itemsIndexed(items = list, key = { _, item -> item.songId }) { _, item ->
                LibrarySongRow(
                    modifier = Modifier.animateItem(),
                    contentUri = item.contentUri,
                    title = item.songTitle,
                    subtitle = item.artist,
                    songId = item.songId,
                    albumId = item.albumId,
                    isSelected = current.songId == item.songId,
                    isFavourite = favorites.contains(item.songId),
                    onClick = {
                        viewModel.playback.ensurePlaybackServiceStarted()
                        if (current.songId != item.songId) viewModel.playback.playFromLibrary(item.songId)
                        navigateToPlayer(item.songId.toString())
                    },
                    onFavouriteClick = { viewModel.playback.toggleFavorite(item.songId) },
                )
            }
        }
    }
}
