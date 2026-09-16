package com.tasnimulhasan.songs

import android.net.Uri
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
    viewModel: SongsViewModel = hiltViewModel()
) {
    val audioList by viewModel.audioList.collectAsStateWithLifecycle()
    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    SongsScreen(
        modifier = modifier,
        audioList = audioList,
        selectedId = currentSelectedAudio.songId,
        isPlaying = isPlaying,
        favorites = favorites,
        onSongClicked = { songId ->
            viewModel.ensurePlaybackServiceStarted()
            if (currentSelectedAudio.songId != songId) viewModel.playSong(songId)
            navigateToPlayer(songId.toString())
        },
        onFavouriteClicked = viewModel::toggleFavorite,
    )
}

@Composable
internal fun SongsScreen(
    modifier: Modifier = Modifier,
    audioList: List<com.tasnimulhasan.entity.home.MusicEntity>,
    selectedId: Long,
    isPlaying: Boolean,
    favorites: Set<Long>,
    onSongClicked: (Long) -> Unit,
    onFavouriteClicked: (Long) -> Unit,
) {
    if (audioList.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(
            items = audioList,
            key = { _, item -> item.songId }
        ) { _, item ->
            SongCard(
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
                onFavouriteClick = { onFavouriteClicked(item.songId) },
            )
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
    onFavouriteClick: () -> Unit,
) {
    val isSelected = selectedId == songId && isPlaying
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CardBlueMediumTextColor else MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .padding(vertical = 6.dp, horizontal = 16.dp)
            .combinedClickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp),
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
                        fontFamily = RobotoFontFamily
                    ),
                    maxLines = 1
                )
                Text(
                    text = artist,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = if (isSelected) WhiteOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = RobotoFontFamily
                    ),
                    maxLines = 1
                )
                Text(
                    text = formatDuration(duration.toLongOrNull() ?: 0L),
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = if (isSelected) WhiteOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = RobotoFontFamily
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
    val df = SimpleDateFormat("mm:ss", Locale.US)
    return df.format(durationMs)
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
