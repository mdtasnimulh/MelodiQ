package com.tasnimulhasan.songs

import com.tasnimulhasan.designsystem.theme.LocalAppFontFamily
import com.tasnimulhasan.ui.motion.entrance

import com.tasnimulhasan.ui.motion.pressFeedback

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.tasnimulhasan.ui.component.AddToPlaylistSheet
import com.tasnimulhasan.ui.component.SongActionsSheet
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.tasnimulhasan.designsystem.component.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tasnimulhasan.designsystem.theme.CardBlueMediumTextColor
import com.tasnimulhasan.designsystem.theme.RobotoFontFamily
import com.tasnimulhasan.designsystem.theme.WhiteOrange
import com.tasnimulhasan.ui.image.AlbumArt
import java.text.SimpleDateFormat
import java.util.Locale
import com.tasnimulhasan.designsystem.R as Res

@Composable
internal fun SongsRouteScreen(
    modifier: Modifier = Modifier,
    navigateToPlayer: (musicId: String) -> Unit,
    navigateToSearch: () -> Unit,
    navigateToSongDetails: (Long) -> Unit,
    viewModel: SongsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    val audioList by viewModel.audioList.collectAsStateWithLifecycle()
    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()

    SongsScreen(
        modifier = modifier,
        audioList = audioList,
        selectedId = currentSelectedAudio.songId,
        isPlaying = isPlaying,
        favorites = favorites,
        sortType = sortType,
        sortTypeLabel = viewModel::sortTypeToDisplayString,
        onSortTypeSelected = viewModel::setSortType,
        onPlayAll = viewModel::playAll,
        onShuffleAll = viewModel::shuffleAll,
        navigateToSearch = navigateToSearch,
        onSongClicked = { songId ->
            viewModel.ensurePlaybackServiceStarted()
            if (currentSelectedAudio.songId != songId) viewModel.playSong(songId)
            navigateToPlayer(songId.toString())
        },
        onFavouriteClicked = viewModel::toggleFavorite,
        playlists = playlists,
        onPlayNext = viewModel::playNext,
        onPlayLater = viewModel::playLater,
        onAddToPlaylist = viewModel::addToPlaylist,
        onCreatePlaylist = viewModel::createPlaylistAndAdd,
        onSongInfo = { navigateToSongDetails(it.songId) },
    )
}

@Composable
internal fun SongsScreen(
    modifier: Modifier = Modifier,
    audioList: List<com.tasnimulhasan.entity.home.MusicEntity>,
    selectedId: Long,
    isPlaying: Boolean,
    favorites: Set<Long>,
    sortType: com.tasnimulhasan.entity.enums.SortType = com.tasnimulhasan.entity.enums.SortType.DATE_MODIFIED_DESC,
    sortTypeLabel: (com.tasnimulhasan.entity.enums.SortType) -> String = { it.name },
    onSortTypeSelected: (com.tasnimulhasan.entity.enums.SortType) -> Unit = {},
    onPlayAll: () -> Unit = {},
    onShuffleAll: () -> Unit = {},
    navigateToSearch: () -> Unit = {},
    onSongClicked: (Long) -> Unit,
    onFavouriteClicked: (Long) -> Unit,
    playlists: List<PlaylistEntity> = emptyList(),
    onPlayNext: (MusicEntity) -> Unit = {},
    onPlayLater: (MusicEntity) -> Unit = {},
    onAddToPlaylist: (playlistId: Int, song: MusicEntity) -> Unit = { _, _ -> },
    onCreatePlaylist: (name: String, song: MusicEntity) -> Unit = { _, _ -> },
    onSongInfo: (MusicEntity) -> Unit = {},
) {
    if (audioList.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var showSortMenu by remember { mutableStateOf(false) }
    // Non-null = that sheet is showing for that song (same pattern as Home).
    var actionsSong by remember { mutableStateOf<MusicEntity?>(null) }
    var playlistSong by remember { mutableStateOf<MusicEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = com.tasnimulhasan.designsystem.component.LocalFloatingBarPadding.current),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (audioList.size == 1) "1 song" else "${audioList.size} songs",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onPlayAll, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play all")
                }
                OutlinedButton(onClick = onShuffleAll, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        itemsIndexed(
            items = audioList,
            key = { _, item -> item.songId }
        ) { index, item ->
            SongCard(
                modifier = Modifier.animateItem().entrance(index),
                path = item.contentUri,
                title = item.songTitle,
                artist = item.artist,
                duration = item.duration,
                songId = item.songId,
                albumId = item.albumId,
                selectedId = selectedId,
                isPlaying = isPlaying,
                isFavourite = favorites.contains(item.songId),
                onClick = { onSongClicked(item.songId) },
                onLongClick = { actionsSong = item },
                onFavouriteClick = { onFavouriteClicked(item.songId) },
            )
        }
    }

    SongActionsSheet(
        song = actionsSong,
        onPlayNext = { onPlayNext(it); actionsSong = null },
        onPlayLater = { onPlayLater(it); actionsSong = null },
        onAddToPlaylist = { actionsSong = null; playlistSong = it },
        onSongInfo = { actionsSong = null; onSongInfo(it) },
        onDismiss = { actionsSong = null },
    )

    AddToPlaylistSheet(
        song = playlistSong,
        playlists = playlists,
        onPlaylistSelected = { song, playlist -> onAddToPlaylist(playlist.id, song); playlistSong = null },
        onCreatePlaylist = { song, name -> onCreatePlaylist(name, song); playlistSong = null },
        onDismiss = { playlistSong = null },
    )

    if (showSortMenu) {
        val options = com.tasnimulhasan.entity.enums.SortType.entries.toList()
        com.tasnimulhasan.ui.component.MelodiqSheet(onDismiss = { showSortMenu = false }) { dismiss ->
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            options.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSortTypeSelected(option)
                            dismiss()
                        }
                        .padding(vertical = 2.dp)
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = sortType == option,
                        onClick = {
                            onSortTypeSelected(option)
                            dismiss()
                        }
                    )
                    Text(text = sortTypeLabel(option))
                }
            }
        }
    }
}

@Composable
private fun SongCard(
    modifier: Modifier = Modifier,
    path: Uri,
    title: String,
    artist: String,
    duration: String,
    songId: Long,
    albumId: Long,
    selectedId: Long,
    isPlaying: Boolean,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavouriteClick: () -> Unit,
) {
    val isSelected = selectedId == songId
    Card(
        border = com.tasnimulhasan.designsystem.theme.glassBorder(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CardBlueMediumTextColor else MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier
            .pressFeedback()
            .fillMaxWidth()
            .height(88.dp)
            .padding(vertical = 6.dp, horizontal = 16.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(com.tasnimulhasan.designsystem.theme.glassElevation(2.dp)),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = AlbumArt(songId = songId, contentUri = path, albumId = albumId),
                contentDescription = "Cover art",
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(Res.drawable.default_cover),
                error = painterResource(Res.drawable.default_cover),
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = if (isSelected) WhiteOrange else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontFamily = LocalAppFontFamily.current ?: RobotoFontFamily
                    ),
                    maxLines = 1
                )
                Text(
                    text = artist,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = if (isSelected) WhiteOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = LocalAppFontFamily.current ?: RobotoFontFamily
                    ),
                    maxLines = 1
                )
                Text(
                    text = formatDuration(duration.toLongOrNull() ?: 0L),
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = if (isSelected) WhiteOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = LocalAppFontFamily.current ?: RobotoFontFamily
                    )
                )
            }

            IconButton(onClick = onFavouriteClick, modifier = Modifier.padding(end = 8.dp)) {
                Icon(
                    imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Filled.HeartBroken,
                    contentDescription = "Favourite Icon",
                    tint = if (isFavourite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val total = (durationMs / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(java.util.Locale.US, "%02d:%02d", m, s)
}

@Preview(showBackground = true)
@Composable
fun SongsScreenPreview() {
    SongsScreen(
        audioList = emptyList(),
        selectedId = 0L,
        isPlaying = false,
        favorites = emptySet(),
        onSongClicked = {},
        onFavouriteClicked = {},
    )
}
