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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.common.service.MelodiqPlayerService
import com.tasnimulhasan.home.components.MusicCard
import timber.log.Timber

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
    Timber.e("CheckIsServiceRunning: ${context.isServiceRunning(MelodiqPlayerService::class.java)}")
    var isServiceRunning by remember { mutableStateOf(false) }
    val audioList by viewModel.audioList.collectAsStateWithLifecycle()
    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
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
                    onMusicClicked = {
                        viewModel.onUiEvents(UIEvents.SelectedAudioChange(index))
                        viewModel.onUiEvents(UIEvents.PlayPause)
                        if (!isServiceRunning) {
                            val intent = Intent(context, MelodiqPlayerService::class.java)
                            context.startService(intent)
                            isServiceRunning = true
                        }
                        viewModel.onUiEvents(UIEvents.UpdateProgress(progress / 100f))
                        navigateToPlayer(item.songId.toString())
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
                onMiniPlayerClick = {
                    viewModel.onUiEvents(UIEvents.UpdateProgress(progress / 100f))
                    navigateToPlayer(currentSelectedAudio.songId.toString())
                },
                onPlayPauseClick = {
                    viewModel.onUiEvents(UIEvents.PlayPause)
                },
                onNextClick = {
                    viewModel.onUiEvents(UIEvents.SeekToNext)
                }
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