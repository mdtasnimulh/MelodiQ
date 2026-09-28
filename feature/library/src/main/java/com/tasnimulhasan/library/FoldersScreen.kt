package com.tasnimulhasan.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.library.components.LibrarySongRow

@Composable
internal fun FoldersRoute(
    modifier: Modifier = Modifier,
    navigateToPlayer: (musicId: String) -> Unit,
    navigateBack: () -> Unit,
    viewModel: FoldersViewModel = hiltViewModel(),
) {
    val level by viewModel.currentLevel.collectAsStateWithLifecycle()
    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val current by viewModel.playback.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playback.isPlaying.collectAsStateWithLifecycle()
    val favorites by viewModel.playback.favorites.collectAsStateWithLifecycle()

    // Back press pops one folder level instead of leaving the screen, until we're at root.
    BackHandler(enabled = canGoBack) { viewModel.back() }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = level.displayName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (level.songs.isNotEmpty() || level.childFolders.isNotEmpty()) {
                        IconButton(onClick = { viewModel.playAllInCurrentFolder() }) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = "Play all in this folder")
                        }
                    }
                }
            }

            if (level.childFolders.isEmpty() && level.songs.isEmpty()) {
                item {
                    Text(
                        text = "This folder is empty",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(items = level.childFolders, key = { it.folderPath }) { folder ->
                ListItem(
                    headlineContent = { Text(folder.folderPath.substringAfterLast('/')) },
                    supportingContent = { Text("${folder.songCount} songs") },
                    leadingContent = { Icon(Icons.Filled.Folder, contentDescription = null) },
                    modifier = Modifier.clickable { viewModel.open(folder.folderPath) }
                )
            }

            itemsIndexed(items = level.songs, key = { _, item -> item.songId }) { _, item ->
                LibrarySongRow(
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
