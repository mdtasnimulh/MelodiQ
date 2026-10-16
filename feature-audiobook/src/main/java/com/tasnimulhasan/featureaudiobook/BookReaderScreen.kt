@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.tasnimulhasan.featureaudiobook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Renders pages of one PDF with [PdfRenderer]. Only one page may be open at a time, hence the lock. */
private class PdfPages(context: Context, uri: String) {
    private val fd: ParcelFileDescriptor? =
        runCatching { context.contentResolver.openFileDescriptor(Uri.parse(uri), "r") }.getOrNull()
    private val renderer: PdfRenderer? = fd?.let { runCatching { PdfRenderer(it) }.getOrNull() }
    private val lock = Mutex()
    private val cache = object : LruCache<String, Bitmap>(64 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    suspend fun render(index: Int, widthPx: Int): Bitmap? {
        val key = "$index@$widthPx"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            lock.withLock {
                cache.get(key)?.let { return@withLock it }
                val r = renderer ?: return@withLock null
                runCatching {
                    r.openPage(index).use { page ->
                        val h = (widthPx.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(AndroidColor.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        cache.put(key, bmp)
                        bmp
                    }
                }.getOrNull()
            }
        }
    }

    fun close() {
        runCatching { renderer?.close() }
        runCatching { fd?.close() }
        cache.evictAll()
    }
}

@Composable
internal fun BookReaderRouteScreen(
    bookId: String,
    autoPlay: Boolean,
    navigateBack: () -> Unit,
    viewModel: BookReaderViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(bookId) { viewModel.load(bookId, autoPlay) }
    LaunchedEffect(Unit) {
        viewModel.speaker.messages.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    when (val current = ui) {
        ReaderUi.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        ReaderUi.Failed -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("This book couldn't be opened.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = navigateBack) { Text("Go back") }
        }
        is ReaderUi.Ready -> BookReaderScreen(current.book, viewModel.speaker, navigateBack)
    }
}

/**
 * Reads a book inside the app and narrates it. The saved position is both the reading position and
 * the listening position: scrolling moves it, narration moves it, and reopening the book returns to it.
 */
@Composable
internal fun BookReaderScreen(book: Book, speaker: BookSpeaker, onBack: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val state by speaker.state.collectAsStateWithLifecycle()
    val presets by speaker.presets.collectAsStateWithLifecycle()
    val presetIndex by speaker.presetIndex.collectAsStateWithLifecycle()
    val rate by speaker.rate.collectAsStateWithLifecycle()
    val source = remember(book.id) { speaker.source }

    val pdf = remember(book.id) { if (book.isEpub) null else PdfPages(context, book.uri) }
    DisposableEffect(pdf) { onDispose { pdf?.close() } }

    val pageCount = state.pageCount
    if (pageCount <= 0) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This book couldn't be opened.", color = scheme.onSurfaceVariant)
        }
        return
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.page.coerceIn(0, pageCount - 1))
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var restored by remember { mutableStateOf(false) }
    var userMoved by remember { mutableStateOf(false) }
    var showVoices by remember { mutableStateOf(false) }
    val shownPage by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    // Land exactly where the book was left, once that page has been measured.
    LaunchedEffect(Unit) {
        val s = speaker.state.value
        val info = snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == s.page } }
            .filterNotNull().first()
        listState.scrollToItem(s.page, (info.size * s.permille / 1000f).toInt())
        restored = true
    }

    // Only the user's own scrolling moves the saved position (narration moves it itself).
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { if (it is DragInteraction.Start) userMoved = true }
    }
    LaunchedEffect(restored) {
        if (!restored) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .debounce(350)
            .collect { (index, offset) ->
                if (!userMoved) return@collect
                userMoved = false
                if (speaker.state.value.playing) return@collect
                val size = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.size
                    ?.takeIf { it > 0 } ?: return@collect
                speaker.seek(index, (offset * 1000f / size).toInt().coerceIn(0, 1000))
            }
    }

    // Follow the narration, unless the user is scrolling somewhere else.
    LaunchedEffect(state.chunkStart, state.page) {
        if (!restored || !state.playing || state.chunkStart < 0 || listState.isScrollInProgress) return@LaunchedEffect
        listState.scrollToPosition(state.page, state.permille, animate = true)
    }
    // Skips and restarts jump straight to the new place.
    LaunchedEffect(state.seekNonce) {
        if (!restored || state.seekNonce == 0) return@LaunchedEffect
        listState.scrollToPosition(state.page, state.permille, animate = false)
    }

    Column(Modifier.fillMaxSize().background(scheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReaderButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = scheme.onSurface) }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    book.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = scheme.onSurface,
                )
                Text(
                    "Page ${(shownPage + 1).coerceAtMost(pageCount)} of $pageCount",
                    fontSize = 12.sp, color = scheme.onSurfaceVariant,
                )
            }
            ReaderButton(onClick = { zoom = (zoom - 0.5f).coerceAtLeast(1f) }) {
                Icon(Icons.Filled.Remove, if (book.isEpub) "Smaller text" else "Zoom out", tint = scheme.onSurface)
            }
            ReaderButton(onClick = { zoom = (zoom + 0.5f).coerceAtMost(3f) }) {
                Icon(Icons.Filled.Add, if (book.isEpub) "Larger text" else "Zoom in", tint = scheme.onSurface)
            }
        }
        LinearProgressIndicator(
            progress = { ((shownPage + 1).coerceAtMost(pageCount)).toFloat() / pageCount },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = scheme.primary,
            trackColor = scheme.outlineVariant,
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (pdf != null) {
                PdfPagesList(pdf, pageCount, listState, zoom)
            } else {
                EpubPagesList(source, pageCount, listState, zoom, state)
            }
        }

        ListenBar(
            playing = state.playing,
            voiceLabel = presets.getOrNull(presetIndex)?.label ?: "Voice",
            rateLabel = formatRate(rate),
            canPrev = shownPage > 0,
            canNext = shownPage < pageCount - 1,
            onPlayPause = { speaker.togglePlay() },
            onPrev = { speaker.skipPage(-1) },
            onNext = { speaker.skipPage(1) },
            onVoice = { showVoices = true },
        )
    }

    if (showVoices) {
        NarratorDialog(
            presets = presets,
            selected = presetIndex,
            rate = rate,
            onSelect = speaker::selectPreset,
            onPreview = speaker::preview,
            onRate = speaker::setRate,
            onDismiss = { showVoices = false },
        )
    }
}

private suspend fun LazyListState.scrollToPosition(page: Int, permille: Int, animate: Boolean) {
    var info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == page }
    if (info == null) {
        scrollToItem(page)
        info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == page } ?: return
    }
    val viewport = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    val target = (info.size * permille / 1000f - viewport * 0.3f).toInt().coerceAtLeast(0)
    if (animate) animateScrollToItem(page, target) else scrollToItem(page, target)
}

@Composable
private fun PdfPagesList(pdf: PdfPages, pageCount: Int, listState: LazyListState, zoom: Float) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val pageWidth = maxWidth * zoom
        // Render sharp enough for the zoom level, but cap it to keep memory sane.
        val renderPx = with(density) { pageWidth.toPx() }.toInt().coerceIn(200, 2200)
        Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(pageWidth),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(pageCount, key = { it }) { index ->
                    val bitmap by produceState<Bitmap?>(null, index, renderPx) {
                        value = pdf.render(index, renderPx)
                    }
                    val bmp = bitmap
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(Color.White)
                            .aspectRatio(if (bmp != null) bmp.width.toFloat() / bmp.height else 0.707f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page ${index + 1}",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpubPagesList(source: BookSource?, pageCount: Int, listState: LazyListState, zoom: Float, state: SpeechState) {
    val scheme = MaterialTheme.colorScheme
    val fontSize = 15f + (zoom - 1f) * 4f
    val highlight = scheme.primary.copy(alpha = 0.28f)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(pageCount, key = { it }) { index ->
            val text = source?.cachedText(index).orEmpty()
            val hs = if (state.playing && state.page == index) state.chunkStart else -1
            val he = if (hs >= 0) state.chunkEnd else -1
            val annotated = remember(text, hs, he, highlight) {
                buildAnnotatedString {
                    append(text)
                    if (hs >= 0 && he in (hs + 1)..text.length) addStyle(SpanStyle(background = highlight), hs, he)
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.surface)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Text(
                    text = annotated,
                    fontFamily = FontFamily.Serif,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.55f).sp,
                    color = scheme.onSurface,
                )
                Text(
                    text = "${index + 1}",
                    fontSize = 11.sp,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ListenBar(
    playing: Boolean,
    voiceLabel: String,
    rateLabel: String,
    canPrev: Boolean,
    canNext: Boolean,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onVoice: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dim = Color(0xFF8E8E93)
    Row(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(CircleShape)
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarChip(voiceLabel, onVoice)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.SkipPrevious, "Previous page",
                tint = if (canPrev) Color.White else dim.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp).clip(CircleShape).clickable(enabled = canPrev, onClick = onPrev),
            )
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(scheme.primary).clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (playing) "Pause" else "Play",
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Icon(
                Icons.Filled.SkipNext, "Next page",
                tint = if (canNext) Color.White else dim.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp).clip(CircleShape).clickable(enabled = canNext, onClick = onNext),
            )
        }
        BarChip(rateLabel, onVoice)
    }
}

@Composable
private fun BarChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun NarratorDialog(
    presets: List<VoicePreset>,
    selected: Int,
    rate: Float,
    onSelect: (Int) -> Unit,
    onPreview: (Int) -> Unit,
    onRate: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Narrator") },
        text = {
            Column {
                presets.forEachIndexed { index, preset ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(index) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = index == selected, onClick = { onSelect(index) })
                        Text(preset.label, modifier = Modifier.weight(1f), color = scheme.onSurface)
                        TextButton(onClick = { onSelect(index); onPreview(index) }) { Text("Preview") }
                    }
                }
                Text(
                    "Speed",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { r ->
                        val on = kotlin.math.abs(r - rate) < 0.01f
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (on) scheme.primary else scheme.surfaceContainerHigh)
                                .clickable { onRate(r) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                formatRate(r), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = if (on) scheme.onPrimary else scheme.onSurface,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

private fun formatRate(rate: Float): String =
    (if (rate % 1f == 0f) rate.toInt().toString() else "%.2f".format(rate).trimEnd('0')) + "×"

@Composable
private fun ReaderButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
