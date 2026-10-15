package com.tasnimulhasan.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.tasnimulhasan.designsystem.component.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ToggleFavouriteUseCase
import com.tasnimulhasan.domain.localusecase.library.SmartPlaylistUseCases
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.library.components.LibrarySongRow
import com.tasnimulhasan.ui.motion.pressFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val DAY_MS = TimeUnit.DAYS.toMillis(1)
private val LONG_TRACK_MS = TimeUnit.MINUTES.toMillis(6)

/** The auto-built lists. [name] is what travels in the navigation route. "Not played yet"
 * keeps its own earlier screen and is linked from the hub, not listed here. */
enum class SmartPlaylistKind(val title: String, val subtitle: String, val empty: String) {
    MOST_PLAYED_MONTH("Most played this month", "Your top tracks over the last 30 days", "Play a few songs and your monthly favourites will appear here"),
    ON_REPEAT("On repeat", "What you can't stop playing this week", "Nothing on repeat yet this week"),
    RECENTLY_ADDED("Recently added", "New in your library, last 30 days", "No new songs in the last 30 days"),
    LONG_TRACKS("Long tracks", "Over 6 minutes - mixes, live sets, epics", "No tracks longer than 6 minutes"),
    REDISCOVER("Rediscover", "Songs you loved but haven't played in 90 days", "You've played everything recently, nice");

    companion object {
        fun fromName(name: String?): SmartPlaylistKind =
            entries.firstOrNull { it.name == name } ?: MOST_PLAYED_MONTH
    }
}

/** A row in a smart playlist: the song plus an optional trailing label ("12 plays"). */
data class SmartItem(val song: MusicEntity, val trailing: String?)

@HiltViewModel
class SmartPlaylistViewModel @Inject constructor(
    smart: SmartPlaylistUseCases,
    playerUseCases: PlayerUseCases,
    toggleFavouriteUseCase: ToggleFavouriteUseCase,
    observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val kind: SmartPlaylistKind = SmartPlaylistKind.fromName(savedStateHandle.get<String>("kind"))

    val playback = LibraryPlaybackState(playerUseCases, toggleFavouriteUseCase, observeFavouriteIdsUseCase, viewModelScope)

    private fun plays(n: Int) = if (n == 1) "1 play" else "$n plays"

    /** null while the first query runs, so the screen shows a loader rather than "empty". */
    val items: StateFlow<List<SmartItem>?> = run {
        val now = System.currentTimeMillis()
        when (kind) {
            SmartPlaylistKind.MOST_PLAYED_MONTH ->
                smart.mostPlayedSince(now - 30 * DAY_MS).map { rows -> rows.map { SmartItem(it.first, plays(it.second)) } }
            SmartPlaylistKind.ON_REPEAT ->
                smart.mostPlayedSince(now - 7 * DAY_MS, 50)
                    // "On repeat" means actually repeated: a single play isn't a habit.
                    .map { rows -> rows.filter { it.second >= 2 }.map { SmartItem(it.first, plays(it.second)) } }
            SmartPlaylistKind.RECENTLY_ADDED ->
                smart.recentlyAdded(now / 1000 - 30 * DAY_MS / 1000).map { rows -> rows.map { SmartItem(it, null) } }
            SmartPlaylistKind.LONG_TRACKS ->
                smart.longTracks(LONG_TRACK_MS).map { rows ->
                    rows.map { SmartItem(it, formatMinutes(it.duration.toLongOrNull() ?: 0L)) }
                }
            SmartPlaylistKind.REDISCOVER ->
                smart.notPlayedSince(now - 90 * DAY_MS).map { rows -> rows.map { SmartItem(it, null) } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }
}

private fun formatMinutes(ms: Long): String {
    val totalMin = ms / 60_000
    return if (totalMin >= 60) "${totalMin / 60}h ${totalMin % 60}m" else "$totalMin min"
}

// ─── Hub ─────────────────────────────────────────────────────────────────────

@Composable
internal fun SmartPlaylistsHubRoute(
    modifier: Modifier = Modifier,
    navigateToSmartPlaylist: (SmartPlaylistKind) -> Unit,
    navigateToNeverPlayed: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Lists that build themselves from what you listen to.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            )
        }
        SmartPlaylistKind.entries.forEach { kind ->
            item(key = kind.name) {
                SmartCard(kind.icon(), kind.title, kind.subtitle) { navigateToSmartPlaylist(kind) }
            }
        }
        item(key = "never_played") {
            SmartCard(Icons.Filled.NewReleases, "Not played yet", "Songs you have never listened to", navigateToNeverPlayed)
        }
    }
}

private fun SmartPlaylistKind.icon(): ImageVector = when (this) {
    SmartPlaylistKind.MOST_PLAYED_MONTH -> Icons.Filled.CalendarMonth
    SmartPlaylistKind.ON_REPEAT -> Icons.Filled.Repeat
    SmartPlaylistKind.RECENTLY_ADDED -> Icons.Filled.FiberNew
    SmartPlaylistKind.LONG_TRACKS -> Icons.Filled.HourglassEmpty
    SmartPlaylistKind.REDISCOVER -> Icons.Filled.Autorenew
}

@Composable
private fun SmartCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .pressFeedback()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Detail ──────────────────────────────────────────────────────────────────

@Composable
internal fun SmartPlaylistRoute(
    modifier: Modifier = Modifier,
    navigateToPlayer: (musicId: String) -> Unit,
    viewModel: SmartPlaylistViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val current by viewModel.playback.currentSelectedAudio.collectAsStateWithLifecycle()
    val favorites by viewModel.playback.favorites.collectAsStateWithLifecycle()
    val kind = viewModel.kind

    val list = items
    when {
        list == null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        list.isEmpty() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(kind.empty, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
        }
        else -> {
            val songs = list.map { it.song }
            LazyColumn(modifier = modifier.fillMaxSize()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(kind.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                if (songs.size == 1) "1 song" else "${songs.size} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = {
                            val shuffled = songs.shuffled()
                            viewModel.playback.playList(shuffled)
                            navigateToPlayer(shuffled.first().songId.toString())
                        }) { Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle all") }
                        IconButton(onClick = {
                            viewModel.playback.playList(songs)
                            navigateToPlayer(songs.first().songId.toString())
                        }) { Icon(Icons.Filled.PlayCircle, contentDescription = "Play all") }
                    }
                }
                itemsIndexed(items = list, key = { _, item -> item.song.songId }) { index, item ->
                    LibrarySongRow(
                        modifier = Modifier.animateItem(),
                        contentUri = item.song.contentUri,
                        title = item.song.songTitle,
                        subtitle = item.song.artist,
                        songId = item.song.songId,
                        albumId = item.song.albumId,
                        isSelected = current.songId == item.song.songId,
                        isFavourite = favorites.contains(item.song.songId),
                        trailingText = item.trailing,
                        onClick = {
                            // The whole smart list is the queue, starting from the tapped song.
                            viewModel.playback.playList(songs, index)
                            navigateToPlayer(item.song.songId.toString())
                        },
                        onFavouriteClick = { viewModel.playback.toggleFavorite(item.song.songId) },
                    )
                }
            }
        }
    }
}
