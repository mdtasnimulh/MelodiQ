package com.tasnimulhasan.home

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import com.tasnimulhasan.home.components.CompactSongRow
import com.tasnimulhasan.home.components.HomeEmptyState
import com.tasnimulhasan.home.components.HomeSectionHeader
import com.tasnimulhasan.home.components.ListeningStatsHeader
import com.tasnimulhasan.home.components.MostPlayedRow
import com.tasnimulhasan.ui.component.AddToPlaylistSheet
import com.tasnimulhasan.ui.component.SongActionsSheet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Home is now a dashboard - stats, Recently Played, Most Played, Favorites - rather than a
 * second copy of the full sorted song list. The full list, sorting, and Play All/Shuffle now
 * live on the Songs screen, which is where a "show me everything" action belongs; Home still
 * offers a one-tap Shuffle for convenience.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.HomeScreen(
    navigateToPlayer: (String) -> Unit,
    navigateToSongDetails: (Long) -> Unit,
    navigateToSearch: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val audioList by viewModel.audioList.collectAsStateWithLifecycle()
    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val listeningStats by viewModel.listeningStats.collectAsStateWithLifecycle()
    val artistCount by viewModel.artistCount.collectAsStateWithLifecycle()
    val albumCount by viewModel.albumCount.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    // Non-null = that sheet is showing for that song.
    val actionsSong = remember { mutableStateOf<MusicEntity?>(null) }
    val playlistSong = remember { mutableStateOf<MusicEntity?>(null) }

    val snackBarHostState = remember { SnackbarHostState() }
    val playlists = remember { mutableStateOf<List<PlaylistEntity>>(emptyList()) }
    val isLoading = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.action(UiAction.FetchAllPlaylists)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowToast -> scope.launch { snackBarHostState.showSnackbar(event.message) }
                is UiEvent.Loading -> isLoading.value = event.loading
                is UiEvent.DataEmpty -> playlists.value = emptyList()
                is UiEvent.Playlists -> playlists.value = event.playlists
            }
        }
    }

    fun openSongActions(song: MusicEntity) {
        actionsSong.value = song
    }

    fun playFromList(song: MusicEntity) {
        viewModel.ensurePlaybackServiceStarted()
        val index = audioList.indexOfFirst { it.songId == song.songId }
        if (currentSelectedAudio.songId != song.songId && index >= 0) {
            viewModel.onUiEvents(UIEvents.SelectedAudioChange(index))
        }
        navigateToPlayer(song.songId.toString())
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (audioList.isEmpty()) {
            HomeEmptyState()
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(onClick = navigateToSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                    ListeningStatsHeader(
                        stats = listeningStats,
                        artistCount = artistCount,
                        albumCount = albumCount,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { viewModel.playAll() },
                            modifier = Modifier.weight(1f),
                            enabled = audioList.isNotEmpty(),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Play all")
                        }
                        OutlinedButton(
                            onClick = { viewModel.shuffleAll() },
                            modifier = Modifier.weight(1f),
                            enabled = audioList.isNotEmpty(),
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Shuffle")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (recentlyPlayed.isNotEmpty()) {
                    item {
                        HomeSectionHeader("Recently Played", Icons.Filled.History)
                        CompactSongRow(
                            items = recentlyPlayed.map { it.first },
                            selectedId = currentSelectedAudio.songId,
                            isPlaying = isPlaying,
                            subtitleFor = { song ->
                                val ts = recentlyPlayed.firstOrNull { it.first.songId == song.songId }?.second ?: 0L
                                viewModel.relativeTimeAgo(ts)
                            },
                            onClick = { playFromList(it) },
                            onLongClick = { openSongActions(it) },
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }

                if (mostPlayed.isNotEmpty()) {
                    item {
                        HomeSectionHeader("Most Played", Icons.Filled.TrendingUp)
                    }
                    items(mostPlayed, key = { "mp_${it.first.songId}" }) { (song, count) ->
                        val rank = mostPlayed.indexOfFirst { it.first.songId == song.songId } + 1
                        MostPlayedRow(
                            rank = rank,
                            song = song,
                            playCount = count,
                            isSelected = song.songId == currentSelectedAudio.songId,
                            onClick = { playFromList(song) },
                            onLongClick = { openSongActions(song) },
                        )
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }

                if (favoriteSongs.isNotEmpty()) {
                    item {
                        HomeSectionHeader("Favorites", Icons.Filled.Favorite)
                        CompactSongRow(
                            items = favoriteSongs,
                            selectedId = currentSelectedAudio.songId,
                            isPlaying = isPlaying,
                            subtitleFor = { it.artist },
                            onClick = { playFromList(it) },
                            onLongClick = { openSongActions(it) },
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }

        SongActionsSheet(
            song = actionsSong.value,
            onPlayNext = { song ->
                viewModel.playNext(song)
                actionsSong.value = null
            },
            onPlayLater = { song ->
                viewModel.playLater(song)
                actionsSong.value = null
            },
            onAddToPlaylist = { song ->
                actionsSong.value = null
                playlistSong.value = song
            },
            onSongInfo = { song ->
                actionsSong.value = null
                navigateToSongDetails(song.songId)
            },
            onDismiss = { actionsSong.value = null }
        )

        AddToPlaylistSheet(
            song = playlistSong.value,
            playlists = playlists.value,
            onPlaylistSelected = { song, playlist ->
                viewModel.action(UiAction.AddMusicToPlaylist(playlistId = playlist.id, music = song))
                playlistSong.value = null
            },
            onCreatePlaylist = { song, name ->
                viewModel.createPlaylistAndAdd(name, song)
                playlistSong.value = null
            },
            onDismiss = { playlistSong.value = null }
        )
    }
}

private fun Int.dp() = this.dp

@Suppress("DEPRECATION")
fun <T> Context.isServiceRunning(service: Class<T>): Boolean {
    return (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any { it.service.className == service.name }
}
