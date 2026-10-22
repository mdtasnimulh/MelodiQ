package com.tasnimulhasan.playlists.component

import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.tasnimulhasan.designsystem.R as Res
import com.tasnimulhasan.designsystem.component.Icon
import com.tasnimulhasan.entity.room.playlist.PlaylistWithStats
import com.tasnimulhasan.ui.image.AlbumArt
import com.tasnimulhasan.ui.motion.pressFeedback
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min

/** Same glass-shelf colours as the Audio Books shelves. */
private val ShelfColors = listOf(
    Color(0xFFB57EDC), Color(0xFFF08A73), Color(0xFF4DB6AC), Color(0xFFF4A94E),
    Color(0xFF4DA3FF), Color(0xFFFF8FB1), Color(0xFF6FD6A3), Color(0xFF8D99AE),
)

private val EmptyCoverGradients = listOf(
    listOf(Color(0xFF8E2C2C), Color(0xFF3B1E3F)),
    listOf(Color(0xFF1F5F8B), Color(0xFF12263F)),
    listOf(Color(0xFF2E7D5B), Color(0xFF14352B)),
    listOf(Color(0xFFB5651D), Color(0xFF4A2511)),
    listOf(Color(0xFF6A4C93), Color(0xFF2A1B3D)),
)

private data class CoverRef(val songId: Long, val albumId: Long, val uri: String)

private fun parseCovers(raw: String?): List<CoverRef> =
    raw?.lines()?.mapNotNull { line ->
        val p = line.split(':', limit = 3)
        val songId = p.getOrNull(0)?.toLongOrNull()
        val uri = p.getOrNull(2)
        if (songId == null || uri == null) null else CoverRef(songId, p.getOrNull(1)?.toLongOrNull() ?: 0L, uri)
    }.orEmpty()

/**
 * A playlist drawn like an Audio Books shelf: its newest songs' covers fan out from the middle, a
 * coloured glass shelf sits over their lower part (with the playlist's initial and a delete button),
 * and the name and song count sit underneath.
 */
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
    val name = playlist.playlist.playlistName
    val color = ShelfColors[playlist.playlist.id.mod(ShelfColors.size)]
    val covers = remember(playlist.coverRefs) { parseCovers(playlist.coverRefs) }
    // Newest in the centre, the others alternating to its right and left.
    val arranged = remember(covers) {
        val n = covers.size
        val slots = arrayOfNulls<CoverRef>(n)
        val c = (n - 1) / 2
        covers.forEachIndexed { k, ref ->
            val offset = (k + 1) / 2
            slots[if (k == 0) c else if (k % 2 == 1) c + offset else c - offset] = ref
        }
        slots.filterNotNull()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .pressFeedback(0.97f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onPlaylistClicked(playlist.playlist.id, name) },
        ) {
            val coverH = 134.dp
            val coverW = coverH * 0.68f
            val slotsCount = if (arranged.isEmpty()) 3 else arranged.size
            val area = maxWidth - 40.dp
            val step = if (slotsCount > 1) min(coverW.value * 0.62f, (area - coverW).value / (slotsCount - 1)).dp else 0.dp
            val total = coverW + step * (slotsCount - 1)
            val startX = (maxWidth - total) / 2
            val middleX = (maxWidth - coverW) / 2
            val centre = (slotsCount - 1) / 2f

            for (i in 0 until slotsCount) {
                val dist = abs(i - centre)
                val scale = 1f - 0.07f * dist
                val w = coverW * scale
                val h = coverH * scale
                androidx.compose.runtime.key(i) {
                    ShelfCover(
                        ref = arranged.getOrNull(i),
                        fallbackIndex = i,
                        targetX = startX + step * i + (coverW - w) / 2,
                        middleX = middleX + (coverW - w) / 2,
                        width = w,
                        height = h,
                        tilt = (i - centre) * 3.5f,
                        z = 10f - dist * 2f,
                        order = i,
                    )
                }
            }

            // The shelf: coloured glass over the lower part of the covers.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(20f)
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.50f), color.copy(alpha = 0.78f))))
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(28.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp)
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = 0.78f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "\u266A",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = color,
                    )
                }
                if (onDeleteClicked != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.30f))
                            .clickable(onClick = onDeleteClicked),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete playlist",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        Text(
            text = name,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One cover of the fan: springs out from the middle into its place, tilting slightly. */
@Composable
private fun BoxScope.ShelfCover(
    ref: CoverRef?,
    fallbackIndex: Int,
    targetX: Dp,
    middleX: Dp,
    width: Dp,
    height: Dp,
    tilt: Float,
    z: Float,
    order: Int,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(order * 70L)
        shown = true
    }
    val x by animateDpAsState(
        if (shown) targetX else middleX,
        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "fanX",
    )
    val rotation by animateFloatAsState(
        if (shown) tilt else 0f,
        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "fanTilt",
    )
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(300), label = "fanAlpha")
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .offset(x = x, y = (-12).dp)
            .zIndex(z)
            .graphicsLayer {
                rotationZ = rotation
                this.alpha = alpha
            }
            .size(width, height)
            .shadow(10.dp, RoundedCornerShape(7.dp))
            .clip(RoundedCornerShape(7.dp)),
    ) {
        if (ref != null) {
            AsyncImage(
                model = AlbumArt(songId = ref.songId, contentUri = Uri.parse(ref.uri), albumId = ref.albumId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(Res.drawable.default_cover),
                error = painterResource(Res.drawable.default_cover),
            )
        } else {
            val g = EmptyCoverGradients[fallbackIndex.mod(EmptyCoverGradients.size)]
            Box(
                Modifier.fillMaxSize().background(Brush.linearGradient(g)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(28.dp),
                )
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
