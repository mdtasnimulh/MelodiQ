package com.tasnimulhasan.featureplayer.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.entity.lyrics.LyricLine
import com.tasnimulhasan.entity.lyrics.LyricsUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LyricsRoute(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit,
    viewModel: LyricsViewModel = hiltViewModel(),
) {
    val song by viewModel.currentSong.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentLineIndex.collectAsStateWithLifecycle()
    val detectedLanguage by viewModel.detectedLanguage.collectAsStateWithLifecycle()
    val translation by viewModel.translation.collectAsStateWithLifecycle()

    // Same drag-down-to-dismiss physics as the Player screen, so the gesture feels
    // identical everywhere the user might expect a bottom-sheet-style screen.
    val density = LocalDensity.current
    val maxDragDistance = with(density) { 500.dp.toPx() }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val thresholdFraction = 0.6f
    val scope = rememberCoroutineScope()

    LyricsScreen(
        modifier = modifier
            .offset { IntOffset(0, offsetY.toInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        val shouldDismiss = offsetY >= maxDragDistance * thresholdFraction
                        scope.launch {
                            animate(
                                initialValue = offsetY,
                                targetValue = if (shouldDismiss) maxDragDistance else 0f,
                                animationSpec = spring(
                                    dampingRatio = if (shouldDismiss) Spring.DampingRatioNoBouncy else Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { value, _ -> offsetY = value }
                            if (shouldDismiss) onNavigateUp()
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            animate(
                                initialValue = offsetY,
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { value, _ -> offsetY = value }
                        }
                    }
                ) { change, dragAmount ->
                    offsetY = (offsetY + dragAmount).coerceIn(0f, maxDragDistance)
                    change.consume()
                }
            },
        songTitle = song.songTitle,
        artist = song.artist,
        state = lyricsState,
        currentIndex = currentIndex,
        detectedLanguage = detectedLanguage,
        translation = translation,
        onToggleTranslation = viewModel::toggleTranslation,
        onLineClicked = { line -> line.timestampMs?.let { viewModel.seekTo(it) } },
    )
}

/** How long after the user stops scrolling before the lyrics start following the music again. */
private const val AUTO_RESUME_FOLLOW_MS = 3_000L

@Composable
internal fun LyricsScreen(
    modifier: Modifier = Modifier,
    songTitle: String,
    artist: String,
    state: LyricsUiState,
    currentIndex: Int,
    detectedLanguage: DetectedLanguage?,
    translation: TranslationState,
    onToggleTranslation: () -> Unit,
    onLineClicked: (LyricLine) -> Unit,
) {
    val translatedLines = (translation as? TranslationState.Showing)?.lines

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Text(text = songTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(text = artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }

        // Only offered when the lyrics are in a language other than English.
        if (state is LyricsUiState.Found && detectedLanguage != null) {
            TranslationBar(
                detected = detectedLanguage,
                translation = translation,
                onToggle = onToggleTranslation,
            )
        }

        when (state) {
            LyricsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is LyricsUiState.Error -> CenteredMessage(state.message)
            is LyricsUiState.Unavailable -> CenteredMessage(
                if (state.offline) "Lyrics unavailable offline" else "Lyrics unavailable"
            )
            is LyricsUiState.Found -> if (state.isSynced) {
                SyncedLyricsList(
                    lines = state.lines,
                    translatedLines = translatedLines,
                    currentIndex = currentIndex,
                    onLineClicked = onLineClicked,
                )
            } else {
                PlainLyricsView(lines = state.lines, translatedLines = translatedLines)
            }
        }
    }
}

@Composable
private fun TranslationBar(
    detected: DetectedLanguage,
    translation: TranslationState,
    onToggle: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
        if (!detected.canTranslate) {
            Text(
                text = "${detected.displayName} lyrics can't be translated on this device",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        val working = translation is TranslationState.Working
        val showing = translation is TranslationState.Showing
        FilterChip(
            selected = showing,
            onClick = onToggle,
            enabled = !working,
            label = {
                Text(
                    when {
                        working -> "Translating\u2026"
                        showing -> "Showing English"
                        else -> "Translate ${detected.displayName} to English"
                    }
                )
            },
            leadingIcon = {
                if (working) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            },
        )
        if (working) {
            Text(
                text = "The first time for a language this downloads a small language pack.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (translation is TranslationState.Failed) {
            Text(
                text = translation.message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun PlainLyricsView(lines: List<LyricLine>, translatedLines: List<String>?) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
        item {
            // Honest about the limitation: without per-line timestamps there's nothing to
            // follow the music with, so say rather than silently not highlighting.
            Text(
                text = "These lyrics aren't time-synced, so lines can't follow the music.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
        itemsIndexed(lines) { index, line ->
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(text = line.text, style = MaterialTheme.typography.bodyLarge)
                translatedLines?.getOrNull(index)?.takeIf { it.isNotBlank() && it != line.text }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricsList(
    lines: List<LyricLine>,
    translatedLines: List<String>?,
    currentIndex: Int,
    onLineClicked: (LyricLine) -> Unit,
) {
    val listState = rememberLazyListState()

    // The user gets full control the moment they scroll - auto-scroll only resumes once
    // they explicitly tap "Jump to current", never on its own after a manual scroll.
    // isAutoScrolling distinguishes our own animateScrollToItem calls (which also trigger
    // isScrollInProgress) from an actual user drag, so we don't immediately cancel
    // following the instant we programmatically scroll to the current line.
    var followCurrentLine by remember { mutableStateOf(true) }
    var isAutoScrolling by remember { mutableStateOf(false) }

    // Scrolling by hand pauses following so the list never fights the user's finger. Once
    // they let go and leave it alone for a few seconds, following resumes on its own and the
    // list glides back to the line being sung - so a line that has scrolled out of view
    // doesn't stay lost until a button is tapped. A new touch cancels the countdown (the
    // effect restarts whenever isScrollInProgress changes).
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            if (!isAutoScrolling) followCurrentLine = false
        } else if (!followCurrentLine) {
            delay(AUTO_RESUME_FOLLOW_MS.milliseconds)
            followCurrentLine = true
        }
    }

    // Whether the current line is on screen right now, and which way it went if not.
    val currentLineVisible by remember(currentIndex) {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.any { it.index == currentIndex } }
    }
    val currentLineIsAbove by remember(currentIndex) {
        derivedStateOf {
            val first = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            currentIndex < first
        }
    }

    LaunchedEffect(currentIndex, followCurrentLine) {
        if (followCurrentLine && currentIndex >= 0) {
            isAutoScrolling = true
            try {
                // Negative offset leaves the current line about a third of the way down the
                // screen (where the eye rests) instead of pinned to the very top.
                val offset = -(listState.layoutInfo.viewportSize.height / 3)
                listState.animateScrollToItem(index = currentIndex.coerceIn(0, lines.lastIndex), scrollOffset = offset)
            } finally {
                isAutoScrolling = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 120.dp, horizontal = 24.dp),
        ) {
            itemsIndexed(lines) { index, line ->
                LyricLineItem(
                    line = line,
                    translation = translatedLines?.getOrNull(index),
                    isCurrent = index == currentIndex,
                    distanceFromCurrent = if (currentIndex < 0) 3 else abs(index - currentIndex),
                    onClick = {
                        followCurrentLine = true
                        onLineClicked(line)
                    },
                )
            }
        }

        // Only offered while paused AND the current line is actually off-screen; the arrow
        // points the way back to it.
        if (!followCurrentLine && currentIndex >= 0 && !currentLineVisible) {
            ExtendedFloatingActionButton(
                onClick = { followCurrentLine = true },
                icon = {
                    Icon(
                        if (currentLineIsAbove) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null
                    )
                },
                text = { Text("Jump to current") },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

/**
 * One lyric line. The "current" look is color + a springy scale + full opacity, with lines
 * fading the farther they are from the current one. Deliberately NOT a font-size/weight
 * change: that re-flows the text and shifts every line below it each time the highlight
 * moves, which is what made the old version jump around. Scale/alpha are layer properties,
 * so they animate without any relayout.
 */
@Composable
private fun LyricLineItem(
    line: LyricLine,
    translation: String?,
    isCurrent: Boolean,
    distanceFromCurrent: Int,
    onClick: () -> Unit,
) {
    val targetAlpha = when {
        isCurrent -> 1f
        distanceFromCurrent == 1 -> 0.62f
        distanceFromCurrent == 2 -> 0.46f
        else -> 0.34f
    }
    val alpha by animateFloatAsState(targetAlpha, spring(stiffness = Spring.StiffnessMediumLow), label = "lyricAlpha")
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "lyricScale"
    )
    val color by animateColorAsState(
        targetValue = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(250),
        label = "lyricColor"
    )

    // An empty timestamped line is an instrumental break - shown as a note so the highlight
    // has somewhere to go during it instead of lingering on the last sung line.
    val isInterlude = line.text.isBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .padding(vertical = 10.dp)
            .pointerInput(line) { detectTapGestures { onClick() } },
    ) {
        Text(
            text = if (isInterlude) "\u266A" else line.text,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
        translation?.takeIf { it.isNotBlank() && it != line.text }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = color.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
