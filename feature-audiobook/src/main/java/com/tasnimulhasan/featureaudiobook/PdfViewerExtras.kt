package com.tasnimulhasan.featureaudiobook

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasnimulhasan.designsystem.theme.glassSheetColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Pinch / double-tap zoom
// ---------------------------------------------------------------------------------------------

internal const val PdfMaxZoom = 5f

/**
 * Zoom and pan of the PDF viewport. The page list keeps its own vertical scrolling; this adds the
 * scale and the horizontal/vertical shift of what is shown, around the point the fingers are on.
 */
@Stable
internal class PdfZoomState {
    var scale by mutableFloatStateOf(1f)
    var tx by mutableFloatStateOf(0f)
    var ty by mutableFloatStateOf(0f)
    var viewport: IntSize = IntSize.Zero

    /** True while a finger is dragging a text selection: pinching, panning and double-tap zoom are switched off. */
    var locked: Boolean = false

    fun clamp() {
        scale = scale.coerceIn(1f, PdfMaxZoom)
        tx = tx.coerceIn(viewport.width * (1f - scale), 0f)
        ty = ty.coerceIn(viewport.height * (1f - scale), 0f)
    }

    /** Multiplies the scale by [factor], keeping the content under [focal] where it is. */
    fun zoomBy(factor: Float, focal: Offset) {
        val old = scale
        val new = (old * factor).coerceIn(1f, PdfMaxZoom)
        val f = new / old
        tx = focal.x - (focal.x - tx) * f
        ty = focal.y - (focal.y - ty) * f
        scale = new
        clamp()
    }

    fun panBy(dx: Float, dy: Float) {
        tx += dx
        ty += dy
        clamp()
    }
}

/**
 * Two fingers pinch and pan; a double tap zooms in on the spot (or back out). One finger still
 * scrolls the pages - when zoomed in it also moves the view sideways. Taps and long-presses are only
 * observed, never consumed, so text selection underneath keeps working.
 */
internal fun Modifier.pdfZoomable(state: PdfZoomState, scope: CoroutineScope): Modifier = this
    .clipToBounds()
    .onSizeChanged {
        state.viewport = it
        state.clamp()
    }
    .pointerInput(state) {
        var lastTapTime = 0L
        var lastTapPos = Offset.Zero
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var pinching = false
            var moved = false
            var lastTime = down.uptimeMillis
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                lastTime = event.changes.maxOf { it.uptimeMillis }
                val pressed = event.changes.count { it.pressed }
                if (state.locked) {
                    // A selection is being dragged: leave the fingers to the text layer.
                } else if (pressed >= 2) {
                    pinching = true
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    val centroid = event.calculateCentroid(useCurrent = true)
                    if (zoom != 1f || pan != Offset.Zero) {
                        state.zoomBy(zoom, centroid)
                        state.panBy(pan.x, pan.y)
                    }
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                } else if (!pinching && pressed == 1) {
                    val c = event.changes.first { it.pressed }
                    if (!moved && (c.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                    if (state.scale > 1.01f) state.panBy(c.position.x - c.previousPosition.x, 0f)
                }
            } while (event.changes.any { it.pressed })

            if (!state.locked && !pinching && !moved && lastTime - down.uptimeMillis < 300L) {
                if (lastTime - lastTapTime < 320L && (down.position - lastTapPos).getDistance() < 90.dp.toPx()) {
                    lastTapTime = 0L
                    val target = if (state.scale > 1.05f) 1f else 2.5f
                    val focal = down.position
                    scope.launch {
                        animate(state.scale, target, animationSpec = tween(220)) { v, _ ->
                            state.zoomBy(v / state.scale, focal)
                        }
                    }
                } else {
                    lastTapTime = lastTime
                    lastTapPos = down.position
                }
            }
        }
    }
    .graphicsLayer {
        transformOrigin = TransformOrigin(0f, 0f)
        scaleX = state.scale
        scaleY = state.scale
        translationX = state.tx
        translationY = state.ty
    }

/** Inverts a rendered page (white paper -> dark) for night reading. */
internal val PdfInvertFilter: ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
)

// ---------------------------------------------------------------------------------------------
// Fast scroller with page bubble
// ---------------------------------------------------------------------------------------------

/**
 * A thumb on the right edge that appears while the pages move: drag it to fly through the document,
 * with the page number shown beside it. While scrolling a "3 / 120" pill sits at the top.
 */
@Composable
internal fun PdfFastScroller(
    listState: LazyListState,
    pageCount: Int,
    onJumpEnd: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pageCount < 4) return
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    var dragging by remember { mutableStateOf(false) }
    var dragPage by remember { mutableIntStateOf(0) }
    var heightPx by remember { mutableIntStateOf(0) }
    var recentlyScrolled by remember { mutableStateOf(false) }
    val scrolling = listState.isScrollInProgress
    LaunchedEffect(scrolling) {
        if (scrolling) {
            recentlyScrolled = true
        } else {
            delay(1400)
            recentlyScrolled = false
        }
    }
    val firstIdx by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val visible = dragging || recentlyScrolled
    val page = if (dragging) dragPage else firstIdx.coerceIn(0, pageCount - 1)
    val thumbH = 48.dp
    val thumbHPx = with(androidx.compose.ui.platform.LocalDensity.current) { thumbH.toPx() }
    val travel = (heightPx - thumbHPx).coerceAtLeast(0f)
    val thumbY = if (pageCount > 1) (page.toFloat() / (pageCount - 1) * travel).roundToInt() else 0

    fun update(y: Float) {
        if (heightPx <= 0) return
        val frac = ((y - thumbHPx / 2f) / travel.coerceAtLeast(1f)).coerceIn(0f, 1f)
        dragPage = (frac * (pageCount - 1)).roundToInt()
        scope.launch { listState.scrollToItem(dragPage) }
    }

    Box(modifier.fillMaxSize().onSizeChanged { heightPx = it.height }) {
        AnimatedVisibility(
            visible = visible && !dragging,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
        ) {
            PagePill("${page + 1} / $pageCount")
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Box(Modifier.offset { IntOffset(0, thumbY) }) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp)
                        .size(width = 10.dp, height = thumbH)
                        .clip(RoundedCornerShape(5.dp))
                        .background(scheme.primary.copy(alpha = 0.85f)),
                )
                if (dragging) {
                    Box(Modifier.align(Alignment.TopEnd).padding(end = 28.dp, top = 8.dp)) {
                        PagePill("${page + 1} / $pageCount")
                    }
                }
            }
        }
        // The touch strip only exists while the thumb is showing, so it never gets in the way of selecting text.
        if (visible) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(36.dp)
                    .pointerInput(pageCount) {
                        detectVerticalDragGestures(
                            onDragStart = { o ->
                                dragging = true
                                update(o.y)
                            },
                            onDragEnd = {
                                dragging = false
                                onJumpEnd(dragPage)
                            },
                            onDragCancel = { dragging = false },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                update(change.position.y)
                            },
                        )
                    },
            )
        }
    }
}

@Composable
private fun PagePill(text: String) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(Color(0xCC202124))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------------------------------------
// Page thumbnails + bookmarks sheet
// ---------------------------------------------------------------------------------------------

/** All pages as thumbnails (tap to jump), or only the bookmarked ones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfPagesSheet(
    pdf: PdfPages,
    pageCount: Int,
    currentPage: Int,
    bookmarks: Set<Int>,
    startOnBookmarks: Boolean,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(if (startOnBookmarks) 1 else 0) }
    val pages = remember(tab, bookmarks, pageCount) {
        if (tab == 1) bookmarks.filter { it in 0 until pageCount }.sorted() else (0 until pageCount).toList()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = glassSheetColor(BottomSheetDefaults.ContainerColor),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Pages", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("All pages") })
                FilterChip(
                    selected = tab == 1, onClick = { tab = 1 },
                    label = { Text(if (bookmarks.isEmpty()) "Bookmarks" else "Bookmarks (${bookmarks.size})") },
                )
            }
            if (pages.isEmpty()) {
                Text(
                    "No bookmarks yet. Tap the bookmark button at the top to save the page you are on.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                val initial = remember { pages.indexOf(currentPage).coerceAtLeast(0) }
                val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(
                    initialFirstVisibleItemIndex = (initial - 3).coerceAtLeast(0),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                ) {
                    items(pages, key = { it }) { index ->
                        PageThumb(
                            pdf = pdf,
                            index = index,
                            current = index == currentPage,
                            bookmarked = index in bookmarks,
                            onClick = { onJump(index) },
                        )
                    }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PageThumb(pdf: PdfPages, index: Int, current: Boolean, bookmarked: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bmp by produceState<Bitmap?>(null, index) { value = pdf.render(index, 260) }
    Column(
        Modifier.clickable(onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(bmp?.let { it.width.toFloat() / it.height } ?: 0.707f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .border(
                    if (current) 2.5.dp else 1.dp,
                    if (current) scheme.primary else scheme.outlineVariant,
                    RoundedCornerShape(6.dp),
                ),
        ) {
            bmp?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Page ${index + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (bookmarked) {
                Icon(
                    Icons.Filled.Bookmark, contentDescription = "Bookmarked", tint = Color(0xFFE53935),
                    modifier = Modifier.align(Alignment.TopEnd).size(22.dp),
                )
            }
        }
        Text(
            "${index + 1}", fontSize = 12.sp,
            color = if (current) scheme.primary else scheme.onSurfaceVariant,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Search bar
// ---------------------------------------------------------------------------------------------

/** One place a search term was found: its page, where in the page text, and how far down the page it sits (0..1). */
internal class SearchHit(val page: Int, val start: Int, val end: Int, val fraction: Float)

@Composable
internal fun PdfSearchBar(
    query: String,
    onQuery: (String) -> Unit,
    status: String,
    canStep: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close search") }
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text("Search in document") },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onNext() }),
            modifier = Modifier.weight(1f).focusRequester(focus),
        )
        if (status.isNotEmpty()) {
            Text(
                status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        IconButton(onClick = onPrev, enabled = canStep) { Icon(Icons.Filled.KeyboardArrowUp, "Previous match") }
        IconButton(onClick = onNext, enabled = canStep) { Icon(Icons.Filled.KeyboardArrowDown, "Next match") }
    }
}
