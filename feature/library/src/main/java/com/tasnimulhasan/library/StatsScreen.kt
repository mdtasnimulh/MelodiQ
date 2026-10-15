package com.tasnimulhasan.library

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import com.tasnimulhasan.designsystem.component.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ToggleFavouriteUseCase
import com.tasnimulhasan.domain.localusecase.library.ListeningInsightsUseCases
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.library.ArtistPlay
import com.tasnimulhasan.ui.image.AlbumArt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import javax.inject.Inject
import com.tasnimulhasan.designsystem.R as Res

enum class StatsPeriod(val chipLabel: String) {
    WEEK("Week"), MONTH("Month"), YEAR("Year"), ALL("All time")
}

/** One chart bar. [caption] is the long form shown when it is the peak ("Tuesday, 14 Oct"). */
data class StatsBar(val label: String, val caption: String, val minutes: Long)

data class StatsUiState(
    val loaded: Boolean = false,
    val totalPlays: Int = 0,
    val totalMs: Long = 0L,
    val topSongs: List<Pair<MusicEntity, Int>> = emptyList(),
    val topArtists: List<ArtistPlay> = emptyList(),
    val bars: List<StatsBar> = emptyList(),
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    insights: ListeningInsightsUseCases,
    playerUseCases: PlayerUseCases,
    toggleFavouriteUseCase: ToggleFavouriteUseCase,
    observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
) : ViewModel() {
    val playback = LibraryPlaybackState(playerUseCases, toggleFavouriteUseCase, observeFavouriteIdsUseCase, viewModelScope)

    private val _period = MutableStateFlow(StatsPeriod.WEEK)
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    fun setPeriod(period: StatsPeriod) { _period.value = period }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<StatsUiState> = _period.flatMapLatest { p ->
        val since = sinceMillis(p)
        combine(
            insights.totals(since),
            insights.topSongs(since),
            insights.topArtists(since),
            insights.playTimes(chartSinceMillis(p)),
        ) { totals, songs, artists, times ->
            StatsUiState(
                loaded = true,
                totalPlays = totals.totalPlays,
                totalMs = totals.totalDurationMs,
                topSongs = songs,
                topArtists = artists,
                bars = buildBars(p, times.map { it.playedAt to it.durationMs }),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}

// ─── Time helpers ────────────────────────────────────────────────────────────

private val zone: ZoneId get() = ZoneId.systemDefault()
private fun LocalDate.startMillis() = atStartOfDay(zone).toInstant().toEpochMilli()

/** Start of the period the totals / top lists are computed over (0 = all time). */
private fun sinceMillis(p: StatsPeriod): Long {
    val today = LocalDate.now(zone)
    return when (p) {
        StatsPeriod.WEEK -> today.minusDays(6).startMillis()
        StatsPeriod.MONTH -> today.minusDays(29).startMillis()
        StatsPeriod.YEAR -> LocalDate.of(today.year, 1, 1).startMillis()
        StatsPeriod.ALL -> 0L
    }
}

/** "All time" still draws a readable chart: its last 12 months. */
private fun chartSinceMillis(p: StatsPeriod): Long =
    if (p == StatsPeriod.ALL) LocalDate.now(zone).withDayOfMonth(1).minusMonths(11).startMillis() else sinceMillis(p)

private fun buildBars(p: StatsPeriod, plays: List<Pair<Long, Long>>): List<StatsBar> {
    val today = LocalDate.now(zone)
    val locale = Locale.getDefault()
    fun dateOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    return when (p) {
        StatsPeriod.WEEK, StatsPeriod.MONTH -> {
            val days = if (p == StatsPeriod.WEEK) 7 else 30
            val start = today.minusDays(days - 1L)
            val ms = LongArray(days)
            plays.forEach { (at, dur) ->
                val idx = ChronoUnit.DAYS.between(start, dateOf(at)).toInt()
                if (idx in 0 until days) ms[idx] += dur
            }
            List(days) { i ->
                val d = start.plusDays(i.toLong())
                val label = if (p == StatsPeriod.WEEK) d.dayOfWeek.getDisplayName(DateTextStyle.SHORT, locale)
                else if (i == 0 || d.dayOfMonth % 5 == 0) d.dayOfMonth.toString() else ""
                val caption = d.dayOfWeek.getDisplayName(DateTextStyle.FULL, locale) + ", " +
                    d.dayOfMonth + " " + d.month.getDisplayName(DateTextStyle.SHORT, locale)
                StatsBar(label, caption, ms[i] / 60_000)
            }
        }
        StatsPeriod.YEAR, StatsPeriod.ALL -> {
            val startMonth = if (p == StatsPeriod.YEAR) YearMonth.of(today.year, 1)
            else YearMonth.from(today).minusMonths(11)
            val ms = LongArray(12)
            plays.forEach { (at, dur) ->
                val idx = ChronoUnit.MONTHS.between(startMonth, YearMonth.from(dateOf(at))).toInt()
                if (idx in 0 until 12) ms[idx] += dur
            }
            List(12) { i ->
                val m = startMonth.plusMonths(i.toLong())
                StatsBar(
                    label = m.month.getDisplayName(DateTextStyle.NARROW, locale),
                    caption = m.month.getDisplayName(DateTextStyle.FULL, locale) + " " + m.year,
                    minutes = ms[i] / 60_000,
                )
            }
        }
    }
}

private fun formatListening(ms: Long): String {
    val totalMin = ms / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h >= 24 -> "${h / 24}d ${h % 24}h"
        h > 0 -> "${h}h ${m}m"
        else -> "$m min"
    }
}

private fun formatMinutes(min: Long): String =
    if (min >= 60) "${min / 60}h ${min % 60}m" else "$min min"

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
internal fun StatsRoute(
    modifier: Modifier = Modifier,
    navigateToPlayer: (musicId: String) -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val period by viewModel.period.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val year = LocalDate.now().year

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatsPeriod.entries.forEach { p ->
                FilterChip(
                    selected = p == period,
                    onClick = { viewModel.setPeriod(p) },
                    label = { Text(p.chipLabel) },
                )
            }
        }

        if (!state.loaded) {
            Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        HeroCard(period, state, year) {
            val topSong = state.topSongs.firstOrNull()?.first
            val topArtist = state.topArtists.firstOrNull()?.artist
            val text = buildString {
                append(if (period == StatsPeriod.YEAR) "My $year in music on MelodiQ: " else "My listening on MelodiQ (${period.chipLabel.lowercase()}): ")
                append("${formatListening(state.totalMs)} listened, ${state.totalPlays} plays.")
                if (topSong != null) append(" Top song: ${topSong.songTitle}.")
                if (topArtist != null) append(" Top artist: $topArtist.")
            }
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) },
                    "Share your stats",
                )
            )
        }

        if (state.totalPlays == 0) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "No listening in this period yet. Play some music and it will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                )
            }
        } else {
            ChartCard(period, state.bars)

            if (state.topSongs.isNotEmpty()) {
                SectionCard("Top songs") {
                    state.topSongs.forEachIndexed { i, (song, plays) ->
                        RankRow(
                            rank = i + 1,
                            title = song.songTitle,
                            subtitle = song.artist,
                            trailing = if (plays == 1) "1 play" else "$plays plays",
                            cover = song,
                            round = false,
                            onClick = {
                                viewModel.playback.ensurePlaybackServiceStarted()
                                viewModel.playback.playFromLibrary(song.songId)
                                navigateToPlayer(song.songId.toString())
                            },
                        )
                    }
                }
            }

            if (state.topArtists.isNotEmpty()) {
                SectionCard("Top artists") {
                    state.topArtists.forEachIndexed { i, artist ->
                        RankRow(
                            rank = i + 1,
                            title = artist.artist,
                            subtitle = null,
                            trailing = if (artist.playCount == 1) "1 play" else "${artist.playCount} plays",
                            cover = artist.coverSong,
                            round = true,
                            onClick = null,
                        )
                    }
                }
            }
        }

        Text(
            "Listening time counts each play as the full length of the song.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun HeroCard(period: StatsPeriod, state: StatsUiState, year: Int, onShare: () -> Unit) {
    val title = when (period) {
        StatsPeriod.WEEK -> "Last 7 days"
        StatsPeriod.MONTH -> "Last 30 days"
        StatsPeriod.YEAR -> "Your $year in music"
        StatsPeriod.ALL -> "All time"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer)
                )
            )
            .padding(24.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                if (state.totalPlays > 0) {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            Text(
                formatListening(state.totalMs),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "listened · ${state.totalPlays} ${if (state.totalPlays == 1) "play" else "plays"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            if (period == StatsPeriod.YEAR && state.totalPlays > 0) {
                val song = state.topSongs.firstOrNull()?.first
                val artist = state.topArtists.firstOrNull()?.artist
                Spacer(Modifier.height(12.dp))
                if (song != null) HighlightLine("Top song", song.songTitle)
                if (artist != null) HighlightLine("Top artist", artist)
            }
        }
    }
}

@Composable
private fun HighlightLine(label: String, value: String) {
    Row(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            "$label  ",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
        Text(
            value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ChartCard(period: StatsPeriod, bars: List<StatsBar>) {
    val peak = bars.maxByOrNull { it.minutes }
    val max = (peak?.minutes ?: 0L).coerceAtLeast(1L)
    SectionCard(
        title = when (period) {
            StatsPeriod.WEEK -> "Listening time per day"
            StatsPeriod.MONTH -> "Listening time per day"
            StatsPeriod.YEAR -> "Listening time per month"
            StatsPeriod.ALL -> "Listening time, last 12 months"
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { bar ->
                val target = (bar.minutes.toFloat() / max).coerceIn(0.03f, 1f)
                val fraction by animateFloatAsState(target, label = "bar")
                val isPeak = bar === peak && bar.minutes > 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .padding(horizontal = if (bars.size > 14) 1.dp else 4.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (isPeak) MaterialTheme.colorScheme.primary
                            else if (bar.minutes > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            bars.forEach { bar ->
                Text(
                    bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (peak != null && peak.minutes > 0) {
            Text(
                "Peak: ${peak.caption} · ${formatMinutes(peak.minutes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 12.dp),
            )
            content()
        }
    }
}

@Composable
private fun RankRow(
    rank: Int,
    title: String,
    subtitle: String?,
    trailing: String,
    cover: MusicEntity?,
    round: Boolean,
    onClick: (() -> Unit)?,
) {
    val shape = if (round) CircleShape else RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            rank.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        if (cover != null) {
            AsyncImage(
                model = AlbumArt(songId = cover.songId, contentUri = cover.contentUri, albumId = cover.albumId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(Res.drawable.default_cover),
                error = painterResource(Res.drawable.default_cover),
                modifier = Modifier.size(48.dp).clip(shape),
            )
        } else {
            Box(
                modifier = Modifier.size(48.dp).clip(shape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
