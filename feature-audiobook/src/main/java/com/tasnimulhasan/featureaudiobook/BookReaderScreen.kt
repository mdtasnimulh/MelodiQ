package com.tasnimulhasan.featureaudiobook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
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

    val pageCount: Int = renderer?.pageCount ?: 0

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
    navigateBack: () -> Unit,
    viewModel: AudioBookViewModel = hiltViewModel(),
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val book = books.firstOrNull { it.id == bookId }
    if (book == null) {
        Box(Modifier.fillMaxSize())
        return
    }
    BookReaderScreen(book = book, onBack = navigateBack)
}

/** Reads a book inside the app: pages scroll vertically, +/- zooms (and pans sideways when zoomed). */
@Composable
internal fun BookReaderScreen(book: Book, onBack: () -> Unit) {
    val context = LocalContext.current
    val pdf = remember(book.id) { PdfPages(context, book.uri) }
    DisposableEffect(pdf) { onDispose { pdf.close() } }

    val scheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }

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
                if (pdf.pageCount > 0) {
                    Text(
                        "Page ${currentPage.coerceAtMost(pdf.pageCount)} of ${pdf.pageCount}",
                        fontSize = 12.sp, color = scheme.onSurfaceVariant,
                    )
                }
            }
            ReaderButton(onClick = { zoom = (zoom - 0.5f).coerceAtLeast(1f) }) {
                Icon(Icons.Filled.Remove, "Zoom out", tint = scheme.onSurface)
            }
            ReaderButton(onClick = { zoom = (zoom + 0.5f).coerceAtMost(3f) }) {
                Icon(Icons.Filled.Add, "Zoom in", tint = scheme.onSurface)
            }
        }

        if (pdf.pageCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This book couldn't be opened.", color = scheme.onSurfaceVariant)
            }
        } else {
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
                        items(pdf.pageCount, key = { it }) { index ->
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
    }
}

@Composable
private fun ReaderButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
