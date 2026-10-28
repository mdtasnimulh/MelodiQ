package com.tasnimulhasan.playlistdetails.component

import com.tasnimulhasan.ui.motion.pressFeedback

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.tasnimulhasan.designsystem.component.Icon
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
    onMusicLongClicked: (() -> Unit)? = null,
    onFavouriteIconClicked: () -> Unit,
    onRemoveClicked: (() -> Unit)? = null,
) {
    val isSelected = selectedId == songId
    // Uses MaterialTheme.colorScheme (primaryContainer/onPrimaryContainer) instead of fixed
    // hex colors, so the selected row reads correctly in both light and dark theme instead
    // of a navy tile that only looked right in one of them.
    val durationText = remember(duration) { convertLongToReadableDateTime(duration.toLongOrNull() ?: 0L, "mm:ss") }

    Card(
        border = com.tasnimulhasan.designsystem.theme.glassBorder(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .pressFeedback()
            .fillMaxWidth()
            .height(88.dp)
            .padding(vertical = 6.dp, horizontal = 16.dp)
            .combinedClickable(
                onClick = { onMusicClicked.invoke() },
                // null = no long-press action here, so the list can use it to start a drag.
                onLongClick = onMusicLongClicked,
            ),
        elevation = CardDefaults.cardElevation(com.tasnimulhasan.designsystem.theme.glassElevation(0.dp)),
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

            if (onRemoveClicked != null) {
                IconButton(
                    onClick = onRemoveClicked,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove from playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

fun convertLongToReadableDateTime(time: Long, format: String): String {
    if (format != "mm:ss") return SimpleDateFormat(format, Locale.US).format(time)
    // Hours are shown once a track passes 60 minutes (1:55:20 instead of 55:20).
    val total = (time / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(java.util.Locale.US, "%02d:%02d", m, s)
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
