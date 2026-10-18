@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.tasnimulhasan.featureaudiobook

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
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

/** A run of selected words on one page (word indexes, either order). */
private data class ReaderSelection(val page: Int, val anchor: Int, val focus: Int)

/**
 * Reads a book inside the app and narrates it. The saved position is both the reading position and
 * the listening position: scrolling moves it, narration moves it, and reopening the book returns to it.
 * While narrating, the sentence being read and the word being spoken are highlighted on the page.
 */
@Composable
internal fun BookReaderScreen(book: Book, speaker: BookSpeaker, onBack: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val state by speaker.state.collectAsStateWithLifecycle()
    val presets by speaker.presets.collectAsStateWithLifecycle()
    val presetIndex by speaker.presetIndex.collectAsStateWithLifecycle()
    val rate by speaker.rate.collectAsStateWithLifecycle()
    val allLanguages by speaker.allLanguages.collectAsStateWithLifecycle()
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
    var showGoTo by remember { mutableStateOf(false) }
    var showDisplay by remember { mutableStateOf(false) }
    var paperIndex by rememberSaveable { mutableIntStateOf(0) }
    var selection by remember { mutableStateOf<ReaderSelection?>(null) }
    val shownPage by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    // Land exactly where the book was left, once that page has been measured.
    LaunchedEffect(Unit) {
        val s = speaker.state.value
        val info = snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == s.page } }
            .filterNotNull().first()
        val frac = fractionFor(source, book.isEpub, s.page, s.permille)
        listState.scrollToItem(s.page, (info.size * frac).toInt())
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
                val frac = (offset.toFloat() / size).coerceIn(0f, 1f)
                speaker.seek(index, permilleFor(source, book.isEpub, index, frac))
            }
    }

    // Follow the narration, unless the user is scrolling somewhere else.
    LaunchedEffect(state.chunkStart, state.page) {
        if (!restored || !state.playing || state.chunkStart < 0 || listState.isScrollInProgress) return@LaunchedEffect
        val frac = if (book.isEpub) {
            state.permille / 1000f
        } else {
            val c = source?.content(state.page)
            val w = if (c != null) c.words.getOrNull(c.wordAt(state.chunkStart)) else null
            w?.t ?: (state.permille / 1000f)
        }
        listState.followPosition(state.page, frac)
    }
    // Skips and restarts jump straight to the new place.
    LaunchedEffect(state.seekNonce) {
        if (!restored || state.seekNonce == 0) return@LaunchedEffect
        listState.scrollToPosition(state.page, fractionFor(source, book.isEpub, state.page, state.permille), animate = false)
    }

    // Coming back from the voice download screen: pick up any newly installed voices.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { speaker.refreshVoices() }

    val paper = PaperTheme.entries[paperIndex.coerceIn(0, PaperTheme.entries.lastIndex)]
    val canvas = if (book.isEpub) paper.canvas else scheme.surfaceContainer
    Column(Modifier.fillMaxSize().background(canvas)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    book.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp, color = scheme.onSurface,
                )
                Text(
                    "${book.formatLabel} · $pageCount pages",
                    fontSize = 11.sp, color = scheme.onSurfaceVariant, maxLines = 1,
                )
            }
            RoundIconButton(Icons.Filled.TextFields, "Display settings", onClick = { showDisplay = true })
        }
        LinearProgressIndicator(
            progress = { ((shownPage + 1).coerceAtMost(pageCount)).toFloat() / pageCount },
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(3.dp).clip(CircleShape),
            color = scheme.primary,
            trackColor = scheme.outlineVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(6.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (pdf != null) {
                PdfPagesList(
                    pdf = pdf,
                    source = source,
                    pageCount = pageCount,
                    listState = listState,
                    zoom = zoom,
                    state = state,
                    selection = selection,
                    onSelect = { selection = it },
                    onClear = { selection = null },
                )
            } else {
                EpubPagesList(source, pageCount, listState, zoom, state, PaperTheme.entries[paperIndex.coerceIn(0, PaperTheme.entries.lastIndex)])
            }
        }

        val sel = selection
        if (sel != null) {
            val preview by produceState("", sel) {
                value = source?.content(sel.page)?.textBetween(sel.anchor, sel.focus).orEmpty()
            }
            SelectionBar(
                preview = preview,
                onClose = { selection = null },
                onCopy = {
                    scope.launch {
                        val text = source?.content(sel.page)?.textBetween(sel.anchor, sel.focus).orEmpty()
                        if (text.isNotBlank()) {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Book text", text))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        }
                        selection = null
                    }
                },
                onRead = {
                    scope.launch {
                        val c = source?.content(sel.page)
                        val word = c?.words?.getOrNull(minOf(sel.anchor, sel.focus))
                        selection = null
                        if (word != null) speaker.playFrom(sel.page, word.start)
                    }
                },
                onSelectAll = {
                    scope.launch {
                        val c = source?.content(sel.page) ?: return@launch
                        if (c.words.isNotEmpty()) selection = ReaderSelection(sel.page, 0, c.words.lastIndex)
                    }
                },
            )
        } else {
            PlayerDock(
                playing = state.playing,
                page = shownPage.coerceIn(0, pageCount - 1),
                pageCount = pageCount,
                voiceLabel = presets.getOrNull(presetIndex)?.label ?: "Voice",
                rateLabel = formatRate(rate),
                onPlayPause = { speaker.togglePlay() },
                onPrev = { speaker.skipPage(-1) },
                onNext = { speaker.skipPage(1) },
                onVoice = { showVoices = true },
                onSeekPage = { target -> speaker.skipPage(target - state.page) },
                onPageLabel = { showGoTo = true },
            )
        }
    }

    if (showVoices) {
        NarratorDialog(
            presets = presets,
            selected = presetIndex,
            rate = rate,
            onSelect = speaker::selectPreset,
            onPreview = speaker::preview,
            onRate = speaker::setRate,
            allLanguages = allLanguages,
            onAllLanguages = speaker::setAllLanguages,
            onGetVoices = { openVoiceInstaller(context) },
            onDismiss = { showVoices = false },
        )
    }
    if (showDisplay) {
        DisplaySheet(
            isEpub = book.isEpub,
            zoom = zoom,
            onZoom = { zoom = it.coerceIn(1f, 3f) },
            paperIndex = paperIndex,
            onPaper = { paperIndex = it },
            onDismiss = { showDisplay = false },
        )
    }
    if (showGoTo) {
        com.tasnimulhasan.ui.component.MelodiqInputSheet(
            title = "Go to page",
            confirmLabel = "Go",
            label = "Page (1 to $pageCount)",
            message = "You are on page ${(shownPage + 1).coerceAtMost(pageCount)}.",
            onConfirm = { text ->
                text.filter { it.isDigit() }.toIntOrNull()?.let { n ->
                    speaker.skipPage((n - 1).coerceIn(0, pageCount - 1) - state.page)
                }
            },
            onDismiss = { showGoTo = false },
        )
    }
}

/** Where on a page (0..1 of its height) a saved position [permille] falls. PDFs use the real layout of the text. */
private suspend fun fractionFor(source: BookSource?, isEpub: Boolean, page: Int, permille: Int): Float {
    if (isEpub || source == null) return permille / 1000f
    val c = source.content(page)
    if (c.words.isEmpty()) return permille / 1000f
    val offset = c.text.length * permille / 1000
    return c.words.getOrNull(c.wordAt(offset))?.t ?: (permille / 1000f)
}

/** The reverse: the saved position (thousandths of the page text) for a place 0..1 down the page. */
private suspend fun permilleFor(source: BookSource?, isEpub: Boolean, page: Int, fraction: Float): Int {
    val plain = (fraction * 1000f).toInt().coerceIn(0, 1000)
    if (isEpub || source == null) return plain
    val c = source.content(page)
    if (c.words.isEmpty() || c.text.isEmpty()) return plain
    val first = c.words.firstOrNull { it.b >= fraction } ?: return plain
    return (first.start * 1000 / c.text.length).coerceIn(0, 1000)
}

private suspend fun LazyListState.scrollToPosition(page: Int, fraction: Float, animate: Boolean) {
    var info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == page }
    if (info == null) {
        scrollToItem(page)
        info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == page } ?: return
    }
    val viewport = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    val target = (info.size * fraction - viewport * 0.3f).toInt().coerceIn(0, info.size)
    if (animate) animateScrollToItem(page, target) else scrollToItem(page, target)
}

/** Scrolls only when the place being read has drifted out of the upper-middle of the screen. */
private suspend fun LazyListState.followPosition(page: Int, fraction: Float) {
    val info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == page }
    if (info != null) {
        val viewport = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
        val y = info.offset + info.size * fraction
        if (y > viewport * 0.10f && y < viewport * 0.62f) return
    }
    scrollToPosition(page, fraction, animate = true)
}

@Composable
private fun PdfPagesList(
    pdf: PdfPages,
    source: BookSource?,
    pageCount: Int,
    listState: LazyListState,
    zoom: Float,
    state: SpeechState,
    selection: ReaderSelection?,
    onSelect: (ReaderSelection) -> Unit,
    onClear: () -> Unit,
) {
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
                    // The words of the page (with their boxes) make it selectable and followable.
                    val content by produceState<PageContent?>(null, index, source) {
                        value = source?.content(index)
                    }
                    val bmp = bitmap
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
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
                        val words = content?.words.orEmpty()
                        if (bmp != null && words.isNotEmpty()) {
                            val active = state.page == index && state.chunkStart >= 0
                            val sel = selection?.takeIf { it.page == index }
                            PdfTextLayer(
                                words = words,
                                sentenceStart = if (active) state.chunkStart else -1,
                                sentenceEnd = if (active) state.chunkEnd else -1,
                                wordStart = if (active) state.wordStart else -1,
                                wordEnd = if (active) state.wordEnd else -1,
                                selStart = sel?.anchor ?: -1,
                                selEnd = sel?.focus ?: -1,
                                onSelect = { a, f -> onSelect(ReaderSelection(index, a, f)) },
                                onClear = { if (selection?.page == index) onClear() },
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpubPagesList(source: BookSource?, pageCount: Int, listState: LazyListState, zoom: Float, state: SpeechState, paper: PaperTheme) {
    val scheme = MaterialTheme.colorScheme
    val fontSize = 15f + (zoom - 1f) * 4f
    val sentenceBg = scheme.primary.copy(alpha = 0.22f)
    val wordBg = scheme.primary.copy(alpha = 0.55f)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(pageCount, key = { it }) { index ->
            val text = source?.cachedText(index).orEmpty()
            val active = state.page == index && state.chunkStart >= 0
            val ss = if (active) state.chunkStart else -1
            val se = if (active) state.chunkEnd else -1
            val ws = if (active) state.wordStart else -1
            val we = if (active) state.wordEnd else -1
            val annotated = remember(text, ss, se, ws, we, sentenceBg, wordBg) {
                buildAnnotatedString {
                    append(text)
                    if (ss >= 0 && se in (ss + 1)..text.length) addStyle(SpanStyle(background = sentenceBg), ss, se)
                    if (ws >= 0 && we in (ws + 1)..text.length) addStyle(SpanStyle(background = wordBg, fontWeight = FontWeight.SemiBold), ws, we)
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(paper.page)
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                // Selectable like any text: long-press, drag the handles, copy.
                SelectionContainer {
                    Text(
                        text = annotated,
                        fontFamily = FontFamily.Serif,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.65f).sp,
                        color = paper.text,
                    )
                }
                Text(
                    text = "${index + 1}",
                    fontSize = 11.sp,
                    color = paper.text.copy(alpha = 0.5f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }
        }
    }
}

/** Replaces the player while text is selected: what is selected, and what can be done with it. */
@Composable
private fun SelectionBar(
    preview: String,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onRead: () -> Unit,
    onSelectAll: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    DockSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = preview.replace('\n', ' ').ifBlank { "Selected text" },
                color = scheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            RoundIconButton(Icons.Filled.Close, "Clear selection", onClose)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ActionPill(Icons.Filled.ContentCopy, "Copy", onCopy, primary = true, modifier = Modifier.weight(1f))
            ActionPill(Icons.Filled.RecordVoiceOver, "Read", onRead, modifier = Modifier.weight(1f))
            ActionPill(Icons.Filled.SelectAll, "All", onSelectAll, modifier = Modifier.weight(1f))
        }
    }
}

/** The floating player: a page scrubber, then narrator, previous / play / next and speed. */
@Composable
private fun PlayerDock(
    playing: Boolean,
    page: Int,
    pageCount: Int,
    voiceLabel: String,
    rateLabel: String,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onVoice: () -> Unit,
    onSeekPage: (Int) -> Unit,
    onPageLabel: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var scrub by remember { mutableStateOf<Float?>(null) }
    val shown = scrub?.toInt() ?: page
    DockSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Page ${shown + 1} of $pageCount",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = scheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onPageLabel).padding(vertical = 2.dp),
            )
            Spacer(Modifier.weight(1f))
            Text("${((shown + 1) * 100 / pageCount.coerceAtLeast(1))}% read", fontSize = 12.sp, color = scheme.onSurfaceVariant)
        }
        Slider(
            value = scrub ?: page.toFloat(),
            onValueChange = { scrub = it },
            onValueChangeFinished = {
                scrub?.let { onSeekPage(it.toInt().coerceIn(0, pageCount - 1)) }
                scrub = null
            },
            valueRange = 0f..(pageCount - 1).coerceAtLeast(1).toFloat(),
            modifier = Modifier.fillMaxWidth().height(32.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ChipButton(voiceLabel, Icons.Filled.RecordVoiceOver, onVoice, Modifier.weight(1f))
            Row(
                modifier = Modifier.weight(1.4f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(Icons.Filled.SkipPrevious, "Previous page", onPrev, enabled = page > 0)
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(8.dp, CircleShape, spotColor = scheme.primary)
                        .clip(CircleShape)
                        .background(scheme.primary)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = playing,
                        transitionSpec = {
                            (scaleIn(tween(200), initialScale = 0.5f) + fadeIn(tween(200))) togetherWith
                                (scaleOut(tween(120), targetScale = 0.5f) + fadeOut(tween(120)))
                        },
                        label = "playPause",
                    ) { isPlaying ->
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            if (isPlaying) "Pause" else "Listen",
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                RoundIconButton(Icons.Filled.SkipNext, "Next page", onNext, enabled = page < pageCount - 1)
            }
            ChipButton(rateLabel, Icons.Filled.Speed, onVoice, Modifier.weight(1f))
        }
    }
}

/** The raised rounded card the player and the selection actions sit in. */
@Composable
private fun DockSurface(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(14.dp, RoundedCornerShape(30.dp), ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.28f))
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        content = content,
    )
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, enabled: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHighest.copy(alpha = if (enabled) 0.9f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = scheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ChipButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clip(CircleShape)
            .background(scheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = scheme.onSecondaryContainer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ActionPill(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (primary) scheme.primary else scheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val c = if (primary) scheme.onPrimary else scheme.onSecondaryContainer
        Icon(icon, null, tint = c, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = c, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** How EPUB pages look: paper, sepia or night. PDFs keep their own page colours. */
private enum class PaperTheme(val label: String, val page: Color, val text: Color, val canvas: Color) {
    Paper("Paper", Color(0xFFFBFAF7), Color(0xFF1D1B18), Color(0xFFEDEBE6)),
    Sepia("Sepia", Color(0xFFF4ECD8), Color(0xFF4A3B2A), Color(0xFFE6DABF)),
    Night("Night", Color(0xFF1C1C1E), Color(0xFFD8D6D2), Color(0xFF0C0C0D)),
}

@Composable
private fun DisplaySheet(
    isEpub: Boolean,
    zoom: Float,
    onZoom: (Float) -> Unit,
    paperIndex: Int,
    onPaper: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    com.tasnimulhasan.ui.component.MelodiqSheet(onDismiss = onDismiss) { dismiss ->
        Text("Display", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Text(if (isEpub) "Text size" else "Zoom", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            RoundIconButton(Icons.Filled.Remove, "Smaller", { onZoom(zoom - 0.25f) }, enabled = zoom > 1f)
            Slider(
                value = zoom, onValueChange = onZoom, valueRange = 1f..3f,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            RoundIconButton(Icons.Filled.Add, "Larger", { onZoom(zoom + 0.25f) }, enabled = zoom < 3f)
        }
        if (isEpub) {
            Text(
                "Page colour", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PaperTheme.entries.forEachIndexed { i, t ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(t.page)
                            .border(if (i == paperIndex) 2.5.dp else 1.dp, if (i == paperIndex) scheme.primary else scheme.outlineVariant, RoundedCornerShape(18.dp))
                            .clickable { onPaper(i) }
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Aa", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = t.text)
                        Text(t.label, fontSize = 12.sp, color = t.text.copy(alpha = 0.75f))
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        com.tasnimulhasan.ui.component.SheetPrimaryButton(text = "Done", onClick = dismiss)
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
    allLanguages: Boolean,
    onAllLanguages: (Boolean) -> Unit,
    onGetVoices: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    com.tasnimulhasan.ui.component.MelodiqSheet(onDismiss = onDismiss) { dismiss ->
        Text("Narrator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Pick a voice and tap the speaker to hear it.",
            fontSize = 13.sp, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEachIndexed { index, preset ->
                val on = index == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (on) scheme.primaryContainer else scheme.surfaceContainerHigh)
                        .border(if (on) 2.dp else 0.dp, if (on) scheme.primary else Color.Transparent, RoundedCornerShape(18.dp))
                        .clickable { onSelect(index) }
                        .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        preset.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (on) scheme.onPrimaryContainer else scheme.onSurface,
                    )
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(scheme.surface.copy(alpha = 0.6f)).clickable { onSelect(index); onPreview(index) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.VolumeUp, "Preview ${preset.label}", tint = scheme.primary, modifier = Modifier.size(20.dp)) }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Show voices of all languages", modifier = Modifier.weight(1f),
                fontSize = 13.sp, color = scheme.onSurfaceVariant,
            )
            Switch(checked = allLanguages, onCheckedChange = onAllLanguages)
        }
        Text("Speed", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { r ->
                val on = kotlin.math.abs(r - rate) < 0.01f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (on) scheme.primary else scheme.surfaceContainerHigh)
                        .clickable { onRate(r) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(formatRate(r), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (on) scheme.onPrimary else scheme.onSurface)
                }
            }
        }
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.secondaryContainer)
                .clickable(onClick = onGetVoices)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.VolumeUp, null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Get more voices", fontWeight = FontWeight.SemiBold, color = scheme.onSecondaryContainer)
                Text(
                    "Opens your phone's voice downloads",
                    fontSize = 12.sp, color = scheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        com.tasnimulhasan.ui.component.SheetPrimaryButton(text = "Done", onClick = dismiss)
    }
}

/** Opens the speech engine's own "install voice data" screen, or the phone's text-to-speech settings. */
private fun openVoiceInstaller(context: Context) {
    val flags = Intent.FLAG_ACTIVITY_NEW_TASK
    val attempts = listOf(
        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(flags),
        Intent("com.android.settings.TTS_SETTINGS").addFlags(flags),
        Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(flags),
    )
    for (intent in attempts) {
        try {
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // try the next screen
        }
    }
    Toast.makeText(context, "Open Settings > Text-to-speech to add voices", Toast.LENGTH_LONG).show()
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
