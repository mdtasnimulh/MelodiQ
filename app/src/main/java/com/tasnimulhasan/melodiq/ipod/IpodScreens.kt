package com.tasnimulhasan.melodiq.ipod

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.key
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.library.AlbumSummary
import com.tasnimulhasan.ui.image.AlbumArt
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import com.tasnimulhasan.designsystem.R as Res

internal val SelectionBlue = Brush.verticalGradient(listOf(Color(0xFF4FB2F2), Color(0xFF1C7CD0)))

// ─────────────────────────────────────────────────────────────────────────────────────────
// Display frame: black bezel, title bar with play indicator + battery, white content area
// ─────────────────────────────────────────────────────────────────────────────────────────

@Composable
internal fun IpodDisplay(
    title: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val d = LocalIpodTheme.current.display
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(d.bezel)
            .padding(3.dp),
    ) {
        Column(Modifier.fillMaxSize().clip(RoundedCornerShape(9.dp)).background(d.content)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(d.barBrush)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color = d.barText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isPlaying) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Playing", tint = d.barIcon, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                }
                BatteryIcon(level = rememberBatteryLevel(), outline = d.barIcon)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun rememberBatteryLevel(): Float {
    val context = LocalContext.current
    var level by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            val intent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val l = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val s = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (l >= 0 && s > 0) level = l.toFloat() / s
            delay(60_000)
        }
    }
    return level
}

@Composable
private fun BatteryIcon(level: Float, outline: Color) {
    Canvas(Modifier.size(width = 30.dp, height = 14.dp)) {
        val body = Size(size.width - 3.dp.toPx(), size.height)
        drawRoundRect(outline, size = body, cornerRadius = CornerRadius(2.5.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFB4E08A), Color(0xFF5BA02C))),
            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
            size = Size((body.width - 4.dp.toPx()) * level.coerceIn(0.05f, 1f), body.height - 4.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
        drawRoundRect(outline, topLeft = Offset(body.width + 0.5.dp.toPx(), size.height * 0.3f), size = Size(2.dp.toPx(), size.height * 0.4f), cornerRadius = CornerRadius(1.dp.toPx()))
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────
// Lists and menus
// ─────────────────────────────────────────────────────────────────────────────────────────

@Composable
internal fun IpodList(
    entry: IpodEntry,
    rows: List<IpodRow>,
    emptyText: String,
    onRowTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = LocalIpodTheme.current.display.empty, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
        }
        return
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (entry.selected - 2).coerceAtLeast(0))
    KeepSelectedVisible(listState, entry.selected, rows.size)

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        itemsIndexed(rows, key = { _, r -> r.id }) { index, row ->
            IpodRowItem(row, selected = index == entry.selected, onClick = { onRowTap(index) })
        }
    }
}

/** Scrolls just enough to keep the highlighted row on screen as the wheel turns. */
@Composable
private fun KeepSelectedVisible(state: LazyListState, selected: Int, count: Int) {
    LaunchedEffect(selected, count) {
        val info = state.layoutInfo
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) return@LaunchedEffect
        val viewportEnd = info.viewportEndOffset
        val firstFull = visible.firstOrNull { it.offset >= 0 }?.index ?: visible.first().index
        val lastFull = visible.lastOrNull { it.offset + it.size <= viewportEnd }?.index ?: visible.last().index
        when {
            selected < firstFull -> state.animateScrollToItem(selected)
            selected > lastFull -> {
                val fullCount = (lastFull - firstFull + 1).coerceAtLeast(1)
                state.animateScrollToItem((selected - fullCount + 1).coerceAtLeast(0))
            }
        }
    }
}

@Composable
private fun IpodRowItem(row: IpodRow, selected: Boolean, onClick: () -> Unit) {
    val d = LocalIpodTheme.current.display
    val textColor = if (selected) d.textSelected else d.text
    val subColor = if (selected) d.subSelected else d.sub
    val hasArt = row.art != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (hasArt) 52.dp else 40.dp)
            .then(if (selected) Modifier.background(d.selection) else Modifier)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasArt) {
            CoverImage(row.art!!, Modifier.size(52.dp))
        }
        Column(
            modifier = Modifier.weight(1f).padding(start = if (hasArt) 10.dp else 10.dp, end = 4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = row.title,
                color = textColor,
                fontSize = if (hasArt) 16.sp else 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.subtitle != null) {
                Text(
                    text = row.subtitle,
                    color = subColor,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (row.chevron && selected) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = d.textSelected, modifier = Modifier.padding(end = 6.dp).size(26.dp))
        }
    }
}

@Composable
internal fun CoverImage(art: IpodArt, modifier: Modifier = Modifier) {
    AsyncImage(
        model = AlbumArt(songId = art.songId, contentUri = art.uri, albumId = art.albumId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = painterResource(Res.drawable.default_cover),
        error = painterResource(Res.drawable.default_cover),
        modifier = modifier,
    )
}

/** The request a Cover Flow cover uses; prefetching with the very same request fills the memory cache for it. */
private fun coverRequest(context: android.content.Context, art: IpodArt, px: Int): ImageRequest =
    ImageRequest.Builder(context)
        .data(AlbumArt(songId = art.songId, contentUri = art.uri, albumId = art.albumId))
        .size(px)
        .build()

/**
 * A cover and its reflection from ONE image load (the reflection used to load the picture a
 * second time, showing the placeholder again on its own).
 */
@Composable
private fun CoverWithReflection(art: IpodArt?, cover: Dp, coverPx: Int) {
    val placeholder = painterResource(Res.drawable.default_cover)
    val d = LocalIpodTheme.current.display
    if (art == null) {
        Box(Modifier.size(cover).background(d.placeholder), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = d.placeholderIcon, modifier = Modifier.size(cover / 2))
        }
        Box(Modifier.size(width = cover, height = cover * 0.4f)) {
            Box(Modifier.fillMaxSize().background(d.placeholder))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(d.reflectTop, d.reflectBottom))))
        }
        return
    }
    val context = LocalContext.current
    val request = remember(art.songId, art.albumId, coverPx) { coverRequest(context, art, coverPx) }
    val painter = rememberAsyncImagePainter(model = request, placeholder = placeholder, error = placeholder)
    Image(painter, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(cover))
    // Reflection: the bottom of the cover, flipped, fading into the background.
    Box(Modifier.size(width = cover, height = cover * 0.4f)) {
        Box(Modifier.fillMaxSize().clipToBounds().graphicsLayer { scaleY = -1f }) {
            Image(
                painter, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.requiredSize(cover).align(Alignment.BottomCenter),
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(d.reflectTop, d.reflectBottom))))
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────
// Cover Flow
// ─────────────────────────────────────────────────────────────────────────────────────────

/**
 * Cover Flow over any list of [items]: the selected cover large and flat in the middle, the
 * others turned and squeezed together on either side with a fading reflection, and the
 * title / subtitle underneath. Turn the wheel, drag a finger across the display, tap a side
 * cover to move there, tap the middle cover to open it.
 */
@Composable
internal fun CoverFlowView(
    items: List<IpodRow>,
    selected: Int,
    emptyText: String,
    onStep: (Int) -> Unit,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = LocalIpodTheme.current.display.empty, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
        }
        return
    }
    val pos by animateFloatAsState(
        targetValue = selected.toFloat(),
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 260f),
        label = "coverFlowPos",
    )
    val density = LocalDensity.current
    val step by rememberUpdatedState(onStep)
    val activate by rememberUpdatedState(onActivate)

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(LocalIpodTheme.current.display.flowBg)
            .pointerInput(items.size) {
                detectTapGestures { tap ->
                    val centreX = size.width / 2f
                    val half = size.height * 0.30f
                    when {
                        tap.x < centreX - half -> step(-1)
                        tap.x > centreX + half -> step(1)
                        else -> activate()
                    }
                }
            }
            .pointerInput(items.size) {
                var travelled = 0f
                val stepPx = 34.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { travelled = 0f },
                ) { change, dragAmount ->
                    change.consume()
                    // Dragging left brings the covers on the right towards the middle.
                    travelled -= dragAmount
                    while (travelled >= stepPx) { travelled -= stepPx; step(1) }
                    while (travelled <= -stepPx) { travelled += stepPx; step(-1) }
                }
            },
    ) {
        val cover = min(maxWidth * 0.40f, maxHeight * 0.56f)
        val coverPx = with(density) { cover.toPx() }

        // Load the covers around the selection BEFORE the wheel gets there, so a cover that scrolls in is
        // already in memory instead of flashing the default picture first.
        val context = LocalContext.current
        val requested = remember(coverPx) { HashSet<Long>() }
        LaunchedEffect(selected, items.size, coverPx) {
            val loader = context.imageLoader
            val px = coverPx.toInt().coerceAtLeast(1)
            for (i in (selected - 8)..(selected + 8)) {
                val art = items.getOrNull(i)?.art ?: continue
                if (requested.add(art.songId)) loader.enqueue(coverRequest(context, art, px))
            }
        }

        val from = (floor(pos) - 4).toInt().coerceAtLeast(0)
        val to = (ceil(pos) + 4).toInt().coerceAtMost(items.lastIndex)
        // Farthest first so the middle cover is drawn on top of its neighbours.
        (from..to).sortedByDescending { abs(it - pos) }.forEach { i ->
            // Keyed by position in the list: the draw order changes as the wheel turns, and without a key
            // each slot was handed a different cover (and showed the default picture while it reloaded).
            key(i) {
                val d = i - pos
                val near = min(abs(d), 1f)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .size(width = cover, height = cover * 1.4f)
                        .zIndex(-abs(d))
                        .graphicsLayer {
                            // The first neighbour sits just beside the middle cover, the rest are
                            // packed tightly behind it, each turned to face the centre.
                            val gap = coverPx * 0.62f
                            translationX = if (abs(d) <= 1f) d * gap else sign(d) * (gap + (abs(d) - 1f) * coverPx * 0.17f)
                            rotationY = -sign(d) * 64f * near
                            val s = 1f - 0.14f * near
                            scaleX = s
                            scaleY = s
                            cameraDistance = 14f * density.density
                        },
                ) {
                    CoverWithReflection(items[i].art, cover, coverPx.toInt().coerceAtLeast(1))
                }
            }
        }

        val centre = items[selected.coerceIn(0, items.lastIndex)]
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val dc = LocalIpodTheme.current.display
            Text(centre.title, color = dc.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (centre.subtitle != null) {
                Text(centre.subtitle, color = dc.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The menu: a white panel over the left part of the display, with a soft edge on its right. */
@Composable
internal fun IpodMenuPanel(
    entry: IpodEntry,
    rows: List<IpodRow>,
    onRowTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = LocalIpodTheme.current.display
    Box(modifier.fillMaxHeight().fillMaxWidth(0.66f).background(d.menuPanel)) {
        IpodList(entry = entry, rows = rows, emptyText = "", onRowTap = onRowTap)
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(8.dp)
                .fillMaxHeight()
                .offset(x = 8.dp)
                .background(Brush.horizontalGradient(listOf(d.menuEdge, Color.Transparent)))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────
// Now Playing
// ─────────────────────────────────────────────────────────────────────────────────────────

@Composable
internal fun NowPlayingView(
    song: MusicEntity?,
    positionMs: Long,
    durationMs: Long,
    scrubbing: Boolean,
    showVolume: Boolean,
    volume: Float,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = LocalIpodTheme.current.display
    if (song == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nothing playing", color = d.empty, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        return
    }
    Column(modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            BoxWithConstraints(Modifier.weight(0.46f).fillMaxHeight()) {
                val side = min(maxWidth, maxHeight * 0.78f)
                Column(Modifier.align(Alignment.TopStart)) {
                    Column(
                        Modifier.graphicsLayer {
                            rotationY = 16f
                            cameraDistance = 14f * density
                        }
                    ) {
                        CoverImage(song.toArt(), Modifier.size(side))
                        Box(Modifier.size(width = side, height = side * 0.28f)) {
                            CoverImage(song.toArt(), Modifier.size(side).graphicsLayer { scaleY = -1f })
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(d.reflectTop, d.nowReflectEnd))))
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.weight(0.54f).fillMaxHeight().padding(start = 12.dp, top = 6.dp),
            ) {
                Text(song.songTitle, color = d.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(song.artist, color = d.artist, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.album, color = d.artist, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favourite",
                        tint = if (isFavourite) Color(0xFFE5365B) else d.icon,
                        modifier = Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onToggleFavourite).padding(5.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = d.icon,
                        modifier = Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onAddToPlaylist).padding(4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        if (showVolume) {
            Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VolumeMute, contentDescription = null, tint = d.icon, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) { IpodProgressBar(volume, showThumb = false) }
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = d.icon, modifier = Modifier.size(18.dp))
            }
        } else {
            val fraction = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
            Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(positionMs), color = d.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) { IpodProgressBar(fraction, showThumb = scrubbing) }
                Spacer(Modifier.width(8.dp))
                Text("-" + formatTime((durationMs - positionMs).coerceAtLeast(0)), color = d.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun IpodProgressBar(fraction: Float, showThumb: Boolean) {
    val d = LocalIpodTheme.current.display
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val h = 10.dp.toPx()
        val top = (size.height - h) / 2f
        drawRoundRect(d.progressTrack, Offset(0f, top), Size(size.width, h), CornerRadius(2.dp.toPx()))
        val w = size.width * fraction.coerceIn(0f, 1f)
        if (w > 0f) {
            drawRoundRect(
                Brush.verticalGradient(listOf(d.progressFillTop, d.progressFillBottom), startY = top, endY = top + h),
                Offset(0f, top), Size(w, h), CornerRadius(2.dp.toPx()),
            )
        }
        if (showThumb) {
            drawRoundRect(d.thumb, Offset((w - 3.dp.toPx()).coerceAtLeast(0f), 0f), Size(6.dp.toPx(), size.height), CornerRadius(3.dp.toPx()))
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
}
