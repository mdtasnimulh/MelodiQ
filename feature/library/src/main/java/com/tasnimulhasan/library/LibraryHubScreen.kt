package com.tasnimulhasan.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
internal fun LibraryHubRoute(
    modifier: Modifier = Modifier,
    navigateToArtists: () -> Unit,
    navigateToGenres: () -> Unit,
    navigateToFolders: () -> Unit,
    navigateToRecentlyPlayed: () -> Unit,
    navigateToSearch: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LibraryHubRow(Icons.Filled.Search, "Search", "Songs, artists, albums, genres, folders", navigateToSearch)
        LibraryHubRow(Icons.Filled.Person, "Artists", "Browse your library by artist", navigateToArtists)
        LibraryHubRow(Icons.AutoMirrored.Filled.QueueMusic, "Genres", "Browse by genre tag", navigateToGenres)
        LibraryHubRow(Icons.Filled.Folder, "Folders", "Browse the actual folder structure", navigateToFolders)
        LibraryHubRow(Icons.Filled.History, "Recently & Most Played", "Continue listening, top tracks", navigateToRecentlyPlayed)
    }
}

@Composable
private fun LibraryHubRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
