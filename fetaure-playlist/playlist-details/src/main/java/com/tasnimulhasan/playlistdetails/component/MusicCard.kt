package com.tasnimulhasan.playlistdetails.component

import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.tasnimulhasan.ui.image.AlbumArt
import java.text.SimpleDateFormat
import java.util.Locale
import com.tasnimulhasan.designsystem.R as Res

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MusicCard(
    modifier: Modifier = Modifier,
    contentUri: Uri,
    albumId: Long?,
    title: String,
    artist: String,
    duration: String,
    songId: Long,
    selectedId: Long,
    isPlaying: Boolean,
    isFavourite: Boolean,
    onMusicClicked: () -> Unit,
    onMusicLongClicked: () -> Unit,
    onFavouriteIconClicked: () -> Unit,
) {
    val isSelected = selectedId == songId
    // Uses MaterialTheme.colorScheme (primaryContainer/onPrimaryContainer) instead of fixed
    // hex colors, so the selected row reads correctly in both light and dark theme instead
    // of a navy tile that only looked right in one of them.
    val durationText = remember(duration) { convertLongToReadableDateTime(duration.toLongOrNull() ?: 0L, "mm:ss") }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .padding(vertical = 6.dp, horizontal = 16.dp)
            .combinedClickable(
                onClick = { onMusicClicked.invoke() },
                onLongClick = { onMusicLongClicked.invoke() }
            ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = AlbumArt(songId = songId, contentUri = contentUri, albumId = albumId ?: 0L),
                contentDescription = "Cover art",
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(Res.drawable.default_cover),
                error = painterResource(Res.drawable.default_cover)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = { onFavouriteIconClicked.invoke() },
                modifier = Modifier.padding(end = 4.dp),
            ) {
                Icon(
                    imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favourite",
                    tint = if (isFavourite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

fun convertLongToReadableDateTime(time: Long, format: String): String {
    val df = SimpleDateFormat(format, Locale.US)
    return df.format(time)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun MusicCardPreview() {
    SharedTransitionLayout {
        MusicCard(
            contentUri = "".toUri(),
            albumId = 0L,
            title = "Song Title",
            artist = "Artist Name",
            duration = "134654",
            onMusicClicked = {},
            songId = 0L,
            selectedId = 0L,
            isPlaying = true,
            isFavourite = true,
            onFavouriteIconClicked = {},
            onMusicLongClicked = {}
        )
    }
}
