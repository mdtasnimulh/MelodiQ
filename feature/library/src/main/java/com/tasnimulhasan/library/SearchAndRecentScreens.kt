package com.tasnimulhasan.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.library.components.LibrarySectionHeader
import com.tasnimulhasan.library.components.LibrarySongRow

@Composable
internal fun SearchRoute(
    modifier: Modifier = Modifier,
    navigateToPlayer: (musicId: String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val current by viewModel.playback.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playback.isPlaying.collectAsStateWithLifecycle()
    val favorites by viewModel.playback.favorites.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChanged,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search songs, artists, albums, genres, folders...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
        )

        when {
            isSearching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            query.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Search your library",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No results for \"$query\"",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(items = results, key = { _, item -> item.songId }) { _, item ->
                    LibrarySongRow(
                        contentUri = item.contentUri,
                        title = item.songTitle,
                        subtitle = "${item.artist} • ${item.album}",
                        songId = item.songId,
                        albumId = item.albumId,
                        isSelected = current.songId == item.songId && isPlaying,
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
}

@Composable
internal fun RecentlyPlayedRoute(
    modifier: Modifier = Modifier,
    navigateToPlayer: (musicId: String) -> Unit,
    viewModel: RecentlyPlayedViewModel = hiltViewModel(),
) {
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val current by viewModel.playback.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playback.isPlaying.collectAsStateWithLifecycle()
    val favorites by viewModel.playback.favorites.collectAsStateWithLifecycle()

    if (recentlyPlayed.isEmpty() && mostPlayed.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Play something and it'll show up here",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (recentlyPlayed.isNotEmpty()) {
            item { LibrarySectionHeader("Continue Listening") }
            itemsIndexed(items = recentlyPlayed, key = { _, item -> "recent_${item.songId}" }) { _, item ->
                LibrarySongRow(
                    contentUri = item.contentUri,
                    title = item.songTitle,
                    subtitle = item.artist,
                    songId = item.songId,
                    albumId = item.albumId,
                    isSelected = current.songId == item.songId && isPlaying,
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

        if (mostPlayed.isNotEmpty()) {
            item { LibrarySectionHeader("Most Played") }
            itemsIndexed(items = mostPlayed, key = { _, pair -> "most_${pair.first.songId}" }) { _, pair ->
                val (item, count) = pair
                LibrarySongRow(
                    contentUri = item.contentUri,
                    title = item.songTitle,
                    subtitle = item.artist,
                    songId = item.songId,
                    albumId = item.albumId,
                    isSelected = current.songId == item.songId && isPlaying,
                    isFavourite = favorites.contains(item.songId),
                    trailingText = "${count}x",
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
