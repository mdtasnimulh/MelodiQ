package com.tasnimulhasan.playlists.component

import com.tasnimulhasan.ui.motion.pressFeedback

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.tasnimulhasan.designsystem.component.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.entity.room.playlist.PlaylistWithStats
import java.util.concurrent.TimeUnit

@Composable
fun PlaylistCard(
    playlist: PlaylistWithStats,
    onPlaylistClicked: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteClicked: (() -> Unit)? = null,
) {
    val subtitle = remember(playlist.songCount, playlist.totalDurationMs) {
        formatPlaylistSubtitle(playlist.songCount, playlist.totalDurationMs)
    }
    val scheme = MaterialTheme.colorScheme
    // Each playlist gets its own gradient (picked from the theme colours by its id), so a
    // list of them reads as a set of distinct covers instead of identical tiles.
    val palette = listOf(
        scheme.primary to scheme.tertiary,
        scheme.tertiary to scheme.secondary,
        scheme.secondary to scheme.primary,
        scheme.primary to scheme.secondary,
    )
    val (c1, c2) = palette[playlist.playlist.id.mod(palette.size)]
    val shape = RoundedCornerShape(28.dp)

    Card(
        border = com.tasnimulhasan.designsystem.theme.glassBorder(),
        modifier = modifier
            .pressFeedback(0.97f)
            .padding(vertical = 6.dp, horizontal = 16.dp)
            .fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(com.tasnimulhasan.designsystem.theme.glassElevation(0.dp)),
        onClick = { onPlaylistClicked(playlist.playlist.id, playlist.playlist.playlistName) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A stack of cards behind the cover: reads as "a collection" at a glance.
            Box(modifier = Modifier.size(width = 84.dp, height = 78.dp)) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(c2.copy(alpha = 0.35f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(70.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(c1, c2))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.playlist.playlistName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (onDeleteClicked != null) {
                androidx.compose.material3.IconButton(onClick = onDeleteClicked) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete playlist",
                        tint = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatPlaylistSubtitle(songCount: Int, totalDurationMs: Long): String {
    val songsPart = if (songCount == 1) "1 song" else "$songCount songs"
    if (songCount == 0) return songsPart
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(totalDurationMs)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val durationPart = if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
    return "$songsPart \u2022 $durationPart"
}
