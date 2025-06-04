package com.tasnimulhasan.home

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.common.service.MelodiqPlayerService
import com.tasnimulhasan.home.components.MusicCard

@Composable
internal fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    navigateToPlayer: (String) -> Unit,
) {
    HomeScreen(
        context = LocalContext.current,
        viewModel,
        Modifier,
        navigateToPlayer,
    )
}

@Composable
internal fun HomeScreen(
    context: Context,
    viewModel: HomeViewModel,
    modifier: Modifier,
    navigateToPlayer: (String) -> Unit,
) {
    val audioList by viewModel.audioList.collectAsStateWithLifecycle()
    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val progressString by viewModel.progressString.collectAsStateWithLifecycle()

    var isFavourite by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initializeListIfNeeded()
    }

    Box (
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn {
            itemsIndexed(audioList) { index, item ->
                val shouldLoadBitmap = remember(item.songId) { true }
                if (shouldLoadBitmap) {
                    LaunchedEffect(item.songId) {
                        viewModel.loadBitmapIfNeeded(context, index)
                    }
                }
                MusicCard(
                    modifier = modifier,
                    bitmap = item.cover,
                    title = item.songTitle,
                    artist = item.artist,
                    duration = item.duration,
                    songId = item.songId,
                    selectedId = currentSelectedAudio.songId,
                    isFavourite = isFavourite,
                    onMusicClicked = {
                        if (!context.isServiceRunning(MelodiqPlayerService::class.java)) {
                            val intent = Intent(context, MelodiqPlayerService::class.java)
                            ContextCompat.startForegroundService(context, intent)
                        }
                        if (currentSelectedAudio.songId != item.songId) {
                            viewModel.onUiEvents(UIEvents.SelectedAudioChange(index))
                        }
                        navigateToPlayer(item.songId.toString())
                    },
                    onFavouriteIconClicked = {
                        isFavourite = !isFavourite
                    }
                )
            }
        }

        if (currentSelectedAudio.songId != 0L) {
            MiniPlayer2(
                modifier = Modifier.align(Alignment.BottomCenter),
                cover = currentSelectedAudio.cover,
                songTitle = currentSelectedAudio.songTitle,
                progress = progress,
                onProgress = { viewModel.onUiEvents(UIEvents.SeekTo(it)) },
                isPlaying = isPlaying,
                progressString = "$progressString / " + viewModel.convertLongToReadableDateTime(
                    currentSelectedAudio.duration.toLong(),
                    "mm:ss"
                ),
                onMiniPlayerClick = { navigateToPlayer(currentSelectedAudio.songId.toString()) },
                onPlayPauseClick = { viewModel.onUiEvents(UIEvents.PlayPause) },
                onNextClick = { viewModel.onUiEvents(UIEvents.SeekToNext) },
                onPreviousClick = { viewModel.onUiEvents(UIEvents.SeekToPrevious) },
                onSeekNextClick = { viewModel.onUiEvents(UIEvents.Forward) },
                onSeekPreviousClick = { viewModel.onUiEvents(UIEvents.Backward) }
            )
        }
    }
}

@Suppress("DEPRECATION")
fun <T> Context.isServiceRunning(service: Class<T>): Boolean {
    return (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any { it -> it.service.className == service.name }
}