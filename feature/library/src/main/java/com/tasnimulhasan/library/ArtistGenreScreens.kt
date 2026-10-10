package com.tasnimulhasan.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
internal fun ArtistsRoute(
    modifier: Modifier = Modifier,
    onArtistClicked: (String) -> Unit,
    viewModel: ArtistsViewModel = hiltViewModel(),
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()

    if (artists.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No artists found", modifier = Modifier.padding(24.dp))
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = artists, key = { it.artist }) { artist ->
            ListItem(
                headlineContent = { Text(artist.artist) },
                supportingContent = { Text("${artist.songCount} songs") },
                modifier = Modifier.clickable(onClick = { onArtistClicked(artist.artist) })
            )
        }
    }
}

@Composable
internal fun ArtistDetailsRoute(
    modifier: Modifier = Modifier,
    navigateToPlayer: (musicId: String) -> Unit,
    viewModel: ArtistDetailsViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val current by viewModel.playback.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playback.isPlaying.collectAsStateWithLifecycle()
    val favorites by viewModel.playback.favorites.collectAsStateWithLifecycle()

    if (songs.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(items = songs, key = { _, item -> item.songId }) { _, item ->
            LibrarySongRow(
                modifier = Modifier.animateItem(),
                contentUri = item.contentUri,
                title = item.songTitle,
                subtitle = item.album,
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

@Composable
internal fun GenresRoute(
    modifier: Modifier = Modifier,
    onGenreClicked: (String) -> Unit,
    viewModel: GenresViewModel = hiltViewModel(),
) {
    val genres by viewModel.genres.collectAsStateWithLifecycle()

    if (genres.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No genre tags found on your files",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = genres, key = { it.genre }) { genre ->
            ListItem(
                headlineContent = { Text(genre.genre) },
                supportingContent = { Text("${genre.songCount} songs") },
                modifier = Modifier.clickable(onClick = { onGenreClicked(genre.genre) })
            )
        }
    }
}

@Composable
internal fun GenreDetailsRoute(
    modifier: Modifier = Modifier,
    navigateToPlayer: (musicId: String) -> Unit,
    viewModel: GenreDetailsViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val current by viewModel.playback.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playback.isPlaying.collectAsStateWithLifecycle()
    val favorites by viewModel.playback.favorites.collectAsStateWithLifecycle()

    if (songs.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(items = songs, key = { _, item -> item.songId }) { _, item ->
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
