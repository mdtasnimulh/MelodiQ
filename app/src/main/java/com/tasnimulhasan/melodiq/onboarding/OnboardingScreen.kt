package com.tasnimulhasan.melodiq.onboarding

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.designsystem.component.Icon
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/** Remembers that the intro has been seen, so it only appears on the first launch. */
internal object OnboardingPrefs {
    private const val FILE = "melodiq_onboarding"
    private const val KEY = "done"

    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun markDone(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
    }
}

private data class Highlight(val icon: ImageVector, val title: String, val text: String)

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val highlights: List<Highlight>,
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Filled.MusicNote,
        title = "Welcome to MelodiQ",
        subtitle = "A fast, private music player for the songs already on your phone. No account, no internet needed.",
        highlights = listOf(
            Highlight(Icons.Filled.Folder, "Your whole library", "Scans your device once and stays in sync as files change."),
            Highlight(Icons.Filled.Home, "A Home that knows you", "Recently played, most played, recently added and songs you haven't heard yet."),
            Highlight(Icons.Filled.Search, "Find anything", "Browse by song, album, artist or folder, or just search."),
        ),
    ),
    OnboardingPage(
        icon = Icons.Filled.GraphicEq,
        title = "A player that feels great",
        subtitle = "Pick the look you love. Everything can be changed later in Settings.",
        highlights = listOf(
            Highlight(Icons.Filled.MusicNote, "Expressive player", "Half, circle or full cover styles, a draggable mini player, or the iPod click-wheel style (Player B)."),
            Highlight(Icons.Filled.GraphicEq, "Visualizer & lyrics", "Bars, waveform or circular visualizer, with synced lyrics that follow the song."),
            Highlight(Icons.Filled.Palette, "Themes & widgets", "Light, dark or AMOLED, colours from your wallpaper or album art, and five home-screen widgets."),
        ),
    ),
    OnboardingPage(
        icon = Icons.Filled.Equalizer,
        title = "Sound, your way",
        subtitle = "Tune how your music plays and let it take care of the rest.",
        highlights = listOf(
            Highlight(Icons.Filled.Equalizer, "Equalizer & presets", "Shape the sound, or reset to flat in one tap."),
            Highlight(Icons.Filled.Tune, "Crossfade & loudness", "Smooth transitions and even volume between tracks."),
            Highlight(Icons.Filled.Speed, "Speed, pitch & sleep timer", "Change tempo and pitch, and fall asleep to music that fades out by itself."),
        ),
    ),
    OnboardingPage(
        icon = Icons.Filled.AutoAwesome,
        title = "Organise and rediscover",
        subtitle = "Keep what you love close and bring back what you forgot.",
        highlights = listOf(
            Highlight(Icons.AutoMirrored.Filled.QueueMusic, "Playlists & favourites", "Make playlists straight from a song's long-press menu and manage your queue."),
            Highlight(Icons.Filled.BarChart, "Smart playlists & stats", "Lists that build themselves, listening stats and a shareable \"Your year in music\"."),
            Highlight(Icons.Filled.Backup, "Handy tools", "Tag editor, duplicate finder, backup & restore, and app shortcuts."),
        ),
    ),
    OnboardingPage(
        icon = Icons.Filled.Headphones,
        title = "Audio Books",
        subtitle = "Turn the PDF and EPUB books on your phone into audio books.",
        highlights = listOf(
            Highlight(Icons.Filled.MenuBook, "Your books, where they are", "Add PDF or EPUB files from your phone. Nothing is copied into the app."),
            Highlight(Icons.Filled.RecordVoiceOver, "Read aloud", "Listen with four narrator voices, two female and two male, at the speed you like."),
            Highlight(Icons.Filled.Bookmark, "Pick up where you stopped", "MelodiQ remembers the page and the spot on it, for reading and listening."),
        ),
    ),
)

@Composable
internal fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pagerState.currentPage == pages.lastIndex

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!last) TextButton(onClick = onFinish) { Text("Skip") }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
                val distance = ((pagerState.currentPage - index) + pagerState.currentPageOffsetFraction)
                    .absoluteValue.coerceIn(0f, 1f)
                PageContent(pages[index], distance)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { i ->
                    val selected = i == pagerState.currentPage
                    val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            Button(
                onClick = {
                    if (last) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(54.dp),
            ) {
                Text(if (last) "Get started" else "Next", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage, distance: Float) {
    val alpha by animateFloatAsState(1f - distance * 0.6f, label = "alpha")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .graphicsLayer { this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer {
                    val s = 1f - distance * 0.25f
                    scaleX = s
                    scaleY = s
                }
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            page.title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            page.highlights.forEach { HighlightRow(it) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HighlightRow(item: Highlight) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Text(item.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
