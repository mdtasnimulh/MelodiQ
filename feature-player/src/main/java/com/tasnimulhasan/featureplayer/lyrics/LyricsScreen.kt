package com.tasnimulhasan.featureplayer.lyrics

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.entity.lyrics.LyricLine
import com.tasnimulhasan.entity.lyrics.LyricsUiState

@Composable
fun LyricsRoute(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit,
    viewModel: LyricsViewModel = hiltViewModel(),
) {
    val song by viewModel.currentSong.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()

    LyricsScreen(
        modifier = modifier,
        songTitle = song.songTitle,
        artist = song.artist,
        state = lyricsState,
        positionMs = positionMs,
        onLineClicked = { line -> line.timestampMs?.let { viewModel.seekTo(it) } },
    )
}

@Composable
internal fun LyricsScreen(
    modifier: Modifier = Modifier,
    songTitle: String,
    artist: String,
    state: LyricsUiState,
    positionMs: Long,
    onLineClicked: (LyricLine) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = songTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(text = artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
                SyncedLyricsList(lines = state.lines, positionMs = positionMs, onLineClicked = onLineClicked)
            } else {
                PlainLyricsView(lines = state.lines)
            }
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
private fun PlainLyricsView(lines: List<LyricLine>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
        itemsIndexed(lines) { _, line ->
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun SyncedLyricsList(
    lines: List<LyricLine>,
    positionMs: Long,
    onLineClicked: (LyricLine) -> Unit,
) {
    val listState = rememberLazyListState()
    val currentIndex by remember(positionMs) {
        derivedStateOf { currentLyricLineIndex(lines, positionMs) }
    }

    // The user gets full control the moment they scroll - auto-scroll only resumes once
    // they explicitly tap "Jump to current", never on its own after a manual scroll.
    // isAutoScrolling distinguishes our own animateScrollToItem calls (which also trigger
    // isScrollInProgress) from an actual user drag, so we don't immediately cancel
    // following the instant we programmatically scroll to the current line.
    var followCurrentLine by remember { mutableStateOf(true) }
    var isAutoScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !isAutoScrolling) {
            followCurrentLine = false
        }
    }

    LaunchedEffect(currentIndex, followCurrentLine) {
        if (followCurrentLine && currentIndex >= 0) {
            isAutoScrolling = true
            listState.animateScrollToItem(index = currentIndex.coerceIn(0, lines.lastIndex))
            isAutoScrolling = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 120.dp, horizontal = 24.dp),
        ) {
            itemsIndexed(lines) { index, line ->
                val isCurrent = index == currentIndex
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (isCurrent) 22.sp else 18.sp,
                    ),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .pointerInput(line) {
                            detectTapGestures { onLineClicked(line) }
                        },
                )
            }
        }

        if (!followCurrentLine) {
            ExtendedFloatingActionButton(
                onClick = { followCurrentLine = true },
                icon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                text = { Text("Jump to current") },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
    }
}
