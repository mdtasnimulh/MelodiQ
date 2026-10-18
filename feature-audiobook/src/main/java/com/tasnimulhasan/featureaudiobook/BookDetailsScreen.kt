package com.tasnimulhasan.featureaudiobook

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.designsystem.component.Icon
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

@Composable
internal fun BookDetailsRouteScreen(
    bookId: String,
    navigateBack: () -> Unit,
    navigateToReader: (String, Boolean) -> Unit,
    viewModel: AudioBookViewModel = hiltViewModel(),
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val book = books.firstOrNull { it.id == bookId }
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    if (book == null) {
        Box(Modifier.fillMaxSize())
        return
    }
    BookDetailsScreen(
        book = book,
        covers = viewModel.covers,
        onToggleFavourite = { viewModel.toggleFavourite(book.id) },
        onRemove = {
            viewModel.remove(book.id)
            navigateBack()
        },
        progress = progress[book.id] ?: Progress(),
        onRead = { navigateToReader(book.id, false) },
        onListen = { navigateToReader(book.id, true) },
        onBack = navigateBack,
    )
}

/**
 * Book details: cover, progress, and the entry points to reading and listening.
 */
@Composable
internal fun BookDetailsScreen(
    book: Book,
    covers: BookCovers,
    onToggleFavourite: () -> Unit,
    onRemove: () -> Unit,
    progress: Progress,
    onRead: () -> Unit,
    onListen: () -> Unit,
    onBack: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val started = progress.page > 0 || progress.permille > 0
    val currentPage = progress.page.coerceIn(0, (book.pages - 1).coerceAtLeast(0)) + 1
    val fraction by animateFloatAsState(
        targetValue = if (book.pages <= 0) 0f else ((progress.page + progress.permille / 1000f) / book.pages).coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "bookProgress",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The app bar is hidden on this screen, so it carries its own back button.
        Row(Modifier.fillMaxWidth()) {
            RoundAction(onClick = onBack, tint = scheme.onSurface) { tint ->
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = tint, modifier = Modifier.size(22.dp))
            }
        }

        // Record-style disc with the cover sitting in front of it.
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            Canvas(Modifier.align(Alignment.TopCenter).size(250.dp)) {
                drawCircle(Color(0xFF16161A))
                listOf(0.92f, 0.82f, 0.72f, 0.62f).forEach {
                    drawCircle(Color.White.copy(alpha = 0.06f), radius = size.minDimension / 2f * it, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                }
                drawCircle(Color(0xFF2A2A30), radius = size.minDimension * 0.12f)
            }
            BookCover(
                book = book,
                covers = covers,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(236.dp)
                    .aspectRatio(0.68f)
                    .shadow(16.dp, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp)),
            )
        }

        Text(
            text = book.title,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            textAlign = TextAlign.Center,
            color = scheme.onBackground,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = if (book.isEpub) "EPUB book" else "PDF document",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatChip(if (book.pages == 1) "1 page" else "${book.pages} pages")
            StatChip(formatSize(book.sizeBytes))
            StatChip(book.formatLabel)
        }

        Row(
            modifier = Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundAction(
                onClick = onToggleFavourite,
                tint = if (book.favourite) Color(0xFFE5365B) else scheme.onSurfaceVariant,
            ) { tint ->
                Icon(if (book.favourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favourite", tint = tint, modifier = Modifier.size(22.dp))
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHigh)
                    .clickable(onClick = onRead)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(if (started) "Continue Reading" else "Read Book", fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
            }
            RoundAction(onClick = onRemove, tint = scheme.onSurfaceVariant) { tint ->
                Icon(Icons.Filled.Delete, "Remove from library", tint = tint, modifier = Modifier.size(22.dp))
            }
        }

        Waveform(
            seed = book.id.hashCode(),
            progress = fraction,
            modifier = Modifier.padding(top = 26.dp).fillMaxWidth().height(48.dp),
            color = scheme.primary,
        )
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Page $currentPage · ${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            Text(if (book.pages == 1) "1 page" else "${book.pages} pages", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }

        // Narration: Play opens the reader and starts reading aloud from where the book was left.
        Row(
            modifier = Modifier
                .padding(top = 14.dp, bottom = 24.dp)
                .fillMaxWidth()
                .height(72.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E))
                .clickable(onClick = onListen)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, "Listen", tint = scheme.onPrimary, modifier = Modifier.size(28.dp))
            }
            Column {
                Text("Listen to this book", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    if (started) "Continues from page $currentPage of ${book.pages}" else "Read aloud with a narrator voice",
                    color = Color(0xFF8E8E93),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun StatChip(text: String) {
    Box(
        Modifier
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RoundAction(onClick: () -> Unit, tint: Color, content: @Composable (Color) -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content(tint) }
}

/** A waveform-style bar: the part already read is solid, the rest is faded. */
@Composable
private fun Waveform(seed: Int, progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val bar = 3.dp.toPx()
        val gap = 3.dp.toPx()
        val count = (size.width / (bar + gap)).toInt().coerceAtLeast(1)
        val played = progress.coerceIn(0f, 1f) * count
        for (i in 0 until count) {
            val v = abs(sin((i + (seed and 0xFF)) * 0.55f) * 0.55f + sin(i * 0.21f + 1f) * 0.35f)
            val h = (0.18f + v.coerceIn(0f, 0.8f)) * size.height
            drawRoundRect(
                color = if (i + 0.5f <= played) color else color.copy(alpha = 0.25f),
                topLeft = Offset(i * (bar + gap), (size.height - h) / 2f),
                size = Size(bar, h),
                cornerRadius = CornerRadius(bar / 2f),
            )
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0L -> "-- MB"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%d KB", bytes / 1024)
    else -> String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
}
