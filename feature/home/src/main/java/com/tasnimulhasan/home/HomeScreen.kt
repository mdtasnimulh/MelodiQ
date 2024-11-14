package com.tasnimulhasan.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tasnimulhasan.designsystem.R
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
        viewModel.initializeListIfNeeded()
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
                            onMusicClicked = { navigateToPlayer(item.songId.toString()) }
                        )
                    }
                }
            }
            is UiState.Error -> {}
        }

        MiniPlayer(
            modifier = Modifier.align(Alignment.BottomCenter),
            cover = viewModel.musics[7].cover,
            songTitle = viewModel.musics[7].songTitle,
            onMiniPlayerClick = {
                navigateToPlayer(viewModel.musics[7].songId.toString())
            }
        )
    }
}