package com.tasnimulhasan.home

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasnimulhasan.home.components.MusicCard

@Composable
internal fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    navigateToPlayer: (String) -> Unit,
) {
    HomeScreen(
        context = LocalContext.current,
        viewModel,
        modifier,
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
    LaunchedEffect(Unit) {
        viewModel.initializeListIfNeeded(context)
    }

    Box (
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = viewModel.uiState.value) {
            is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is UiState.MusicList -> {
                LazyColumn {
                    itemsIndexed(state.musics) { index, item ->
                        LaunchedEffect(Unit) {
                            viewModel.loadBitmapIfNeeded(context, index)
                        }
                        MusicCard(
                            modifier = modifier,
                            bitmap = item.cover,
                            title = item.songTitle,
                            artist = item.artist,
                            duration = item.duration,
                            onMusicClicked = { navigateToPlayer(item.songId.toString()) }
                        )
                    }
                }
            }
            is UiState.Error -> {}
        }
    }
}