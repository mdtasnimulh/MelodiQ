package com.tasnimulhasan.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
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
    var isServiceRunning = false
    LaunchedEffect(Unit) {
        viewModel.initializeListIfNeeded()
    }

    Box (
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn {
            itemsIndexed(viewModel.audioList) { index, item ->
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
                        navigateToPlayer(item.songId.toString())
                    }
                )
            }
        }

        if (viewModel.isPlaying) {
            MiniPlayer2(
                modifier = Modifier.align(Alignment.BottomCenter),
                cover = viewModel.currentSelectedAudio.cover,
                songTitle = viewModel.currentSelectedAudio.songTitle,
                progress = viewModel.progress,
                onProgress = { viewModel.onUiEvents(UIEvents.SeekTo(it)) },
                isPlaying = viewModel.isPlaying,
                onMiniPlayerClick = {
                    viewModel.onUiEvents(UIEvents.UpdateProgress(viewModel.progress / 100f))
                    navigateToPlayer(viewModel.currentSelectedAudio.songId.toString())
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