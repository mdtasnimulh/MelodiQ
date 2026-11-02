package com.tasnimulhasan.ui.component

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tasnimulhasan.designsystem.theme.glassSheetColor
import com.tasnimulhasan.ui.image.AlbumArt
import com.tasnimulhasan.ui.image.AlbumArtAccentExtractor
import com.tasnimulhasan.designsystem.R as Res

// ---------------------------------------------------------------------------------------------
// Shared look for the Album and Playlist detail screens: a gradient taken from the cover, the
// cover in the middle, name and song count, big Play / Shuffle buttons and plain song rows.
// ---------------------------------------------------------------------------------------------

/** How the songs of a detail screen are ordered. */
enum class DetailsSort(val label: String) {
    DEFAULT("Default order"),
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    ARTIST("Artist"),
    DURATION_DESC("Longest first"),
}

fun <T> List<T>.sortedFor(
    sort: DetailsSort,
    title: (T) -> String,
    artist: (T) -> String,
    durationMs: (T) -> Long,
): List<T> = when (sort) {
    DetailsSort.DEFAULT -> this
    DetailsSort.TITLE_ASC -> sortedBy { title(it).lowercase() }
    DetailsSort.TITLE_DESC -> sortedByDescending { title(it).lowercase() }
    DetailsSort.ARTIST -> sortedWith(compareBy<T> { artist(it).lowercase() }.thenBy { title(it).lowercase() })
    DetailsSort.DURATION_DESC -> sortedByDescending { durationMs(it) }
}

/** The main colour of a song's cover (falls back to the theme colour while it loads or when there is none). */
@Composable
fun rememberCoverAccent(contentUri: Uri?, albumId: Long): Color {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.primary
    val extracted by produceState<Color?>(null, contentUri, albumId) {
        value = if (contentUri == null) null
        else runCatching { AlbumArtAccentExtractor.extractAccentColor(context, contentUri, albumId) }.getOrNull()
    }
    val target = extracted ?: fallback
    val animated by animateColorAsState(target, label = "coverAccent")
    return animated
}

/** Back button on the left, sort and "more" on the right, floating over the gradient. */
@Composable
fun DetailsTopBar(
    onBack: () -> Unit,
    onSort: (() -> Unit)? = null,
    menuItems: List<Pair<String, () -> Unit>> = emptyList(),
    modifier: Modifier = Modifier,
    withStatusBarPadding: Boolean = true,
    title: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val chip = scheme.surface.copy(alpha = 0.40f)
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (withStatusBarPadding) Modifier.statusBarsPadding() else Modifier),
        contentAlignment = Alignment.Center,
    ) {
    if (title != null) Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = scheme.onSurface,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.padding(horizontal = 76.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(chip).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", tint = scheme.onSurface, modifier = Modifier.size(30.dp))
        }
        if (onSort != null || menuItems.isNotEmpty()) Row(
            Modifier.clip(CircleShape).background(chip),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onSort != null) IconButton(onClick = onSort, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.Sort, "Sort", tint = scheme.onSurface)
            }
            if (menuItems.isNotEmpty()) {
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.MoreVert, "More", tint = scheme.onSurface)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        menuItems.forEach { (label, action) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { menuOpen = false; action() })
                        }
                    }
                }
            }
        }
    }
    }
}

/** The gradient header: cover, name, song count and the Play / Shuffle buttons. Put it first in the list. */
@Composable
fun DetailsHero(
    title: String,
    subtitle: String,
    accent: Color,
    canPlay: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    cover: @Composable () -> Unit,
) {
    val bg = MaterialTheme.colorScheme.background
    val dark = bg.luminance() < 0.5f
    val top = lerp(accent, if (dark) Color.Black else Color.White, if (dark) 0.60f else 0.55f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            // The gradient starts behind the status bar, then fades into the page colour.
            .background(Brush.verticalGradient(0f to top, 0.78f to bg, 1f to bg))
            .statusBarsPadding()
            .padding(top = 64.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        cover()
        Spacer(Modifier.height(26.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(26.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeroButton("Play", Icons.Filled.PlayArrow, canPlay, onPlay, Modifier.weight(1f))
            HeroButton("Shuffle", Icons.Filled.Shuffle, canPlay, onShuffle, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HeroButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = scheme.primary.copy(alpha = if (enabled) 1f else 0.4f)
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHigh)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** A big round cover (album / artist look). */
@Composable
fun HeroCoverCircle(songId: Long, contentUri: Uri, albumId: Long, size: androidx.compose.ui.unit.Dp = 230.dp) {
    AsyncImage(
        model = AlbumArt(songId = songId, contentUri = contentUri, albumId = albumId),
        contentDescription = "Cover art",
        modifier = Modifier.size(size).clip(CircleShape),
        contentScale = ContentScale.Crop,
        placeholder = painterResource(Res.drawable.default_cover),
        error = painterResource(Res.drawable.default_cover),
    )
}

/** A rounded tile in the cover's colour with the round cover inside (playlist look). */
@Composable
fun HeroCoverTile(songId: Long, contentUri: Uri, albumId: Long, accent: Color, size: androidx.compose.ui.unit.Dp = 230.dp) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tile = lerp(accent, if (dark) Color.Black else Color.White, if (dark) 0.45f else 0.35f)
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.2f)).background(tile),
        contentAlignment = Alignment.Center,
    ) {
        HeroCoverCircle(songId, contentUri, albumId, size = size * 0.82f)
    }
}

/** One song: rounded cover, bold title, "artist - album" underneath, optional buttons on the right. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailsSongRow(
    songId: Long,
    contentUri: Uri,
    albumId: Long,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = AlbumArt(songId = songId, contentUri = contentUri, albumId = albumId),
            contentDescription = "Cover art",
            modifier = Modifier.size(62.dp).clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(Res.drawable.default_cover),
            error = painterResource(Res.drawable.default_cover),
        )
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) scheme.primary else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 15.sp,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        trailing()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsSortSheet(current: DetailsSort, onSelect: (DetailsSort) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = glassSheetColor(BottomSheetDefaults.ContainerColor),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Sort by",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            DetailsSort.entries.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option); onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option == current, onClick = { onSelect(option); onDismiss() })
                    Text(option.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
