package com.tasnimulhasan.playlistdetails

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasnimulhasan.common.service.MelodiqPlayerService
import com.tasnimulhasan.domain.localusecase.playlistdetails.GetAllMusicFromPlaylistUseCase
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import com.tasnimulhasan.playlistdetails.component.MusicCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PlaylistDetailsScreen(
    playlistId: Int,
    onNavigateUp: () -> Unit,
    navigateToPlayer: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val musicList = remember { mutableStateOf<List<PlaylistDetailsEntity>>(emptyList()) }
    val isLoading = remember { mutableStateOf(false) }
    val isEmpty = remember { mutableStateOf(false) }
    val snackBarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.action(UiAction.FetchMusicList(GetAllMusicFromPlaylistUseCase.Params(playlistId)))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    scope.launch {
                        snackBarHostState.showSnackbar(event.message)
                    }
                }
                is UiEvent.Loading -> isLoading.value = event.loading
                is UiEvent.DataEmpty -> {
                    isEmpty.value = true
                    musicList.value = emptyList()
                }
                is UiEvent.MusicList -> {
                    isEmpty.value = false
                    musicList.value = event.musicList
                }
            }
        }
    }

    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
    ) {
        val (count, musicCard) = createRefs()

        Text(
            modifier = Modifier
                .constrainAs(count){
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            text = "Total: ${musicList.value.size}",
            style = MaterialTheme.typography.titleLarge
        )

        LazyColumn(
            modifier = Modifier
                .constrainAs(musicCard){
                    top.linkTo(count.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxSize(),
            state = listState
        ) {
            val musicListForPlay = mutableListOf<MusicEntity>()
            musicList.value.forEach { item ->
                musicListForPlay.add(
                    MusicEntity(
                        contentUri = item.contentUri.toUri(),
                        songId = item.songId,
                        cover = viewModel.getAlbumArt(context, item.contentUri.toUri()),
                        songTitle = item.songTitle,
                        artist = item.artist ?: "",
                        duration = item.duration,
                        album = item.album ?: "",
                        albumId = item.albumId ?: 0L
                    )
                )
            }
            itemsIndexed(musicList.value) { index, item ->
                MusicCard(
                    modifier = modifier,
                    bitmap = viewModel.getAlbumArt(context, item.contentUri.toUri()),
                    title = item.songTitle,
                    artist = item.artist ?: "",
                    duration = item.duration,
                    songId = item.songId,
                    selectedId = 0L,
                    isPlaying = context.isServiceRunning(MelodiqPlayerService::class.java),
                    isFavourite = false,
                    onMusicClicked = {
                        navigateToPlayer(item.songId.toString())
                    },
                    onMusicLongClicked = {

                    },
                    onFavouriteIconClicked = {

                    },
                )
            }
        }
    }

}

@Suppress("DEPRECATION")
fun <T> Context.isServiceRunning(service: Class<T>): Boolean {
    return (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any { it -> it.service.className == service.name }
}