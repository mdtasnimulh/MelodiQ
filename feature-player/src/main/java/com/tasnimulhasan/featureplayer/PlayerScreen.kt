package com.tasnimulhasan.featureplayer

import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tasnimulhasan.common.service.MusicPlayerService
import com.tasnimulhasan.entity.home.MusicEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import com.tasnimulhasan.designsystem.R as Res

@Composable
internal fun PlayerScreen(
    musicId: String,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState { viewModel.musics.size }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isPlaying = MutableStateFlow(false)
    val maxDuration = MutableStateFlow(0f)
    val currentDuration = MutableStateFlow(0f)
    val currentTrack = MutableStateFlow<MusicEntity?>(null)
    var musicPlayerService: MusicPlayerService? = null
    var isBound = false

    val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            musicPlayerService = (binder as MusicPlayerService.MusicBinder).getService()
            binder.setMusicList(viewModel.musics)
            scope.launch {
                musicPlayerService?.play(currentTrack.value ?: viewModel.musics.first())
            }
            scope.launch {
                binder.isPlaying().collectLatest {
                    isPlaying.value = it
                }
            }
            scope.launch {
                binder.maxDuration().collectLatest {
                    maxDuration.value = it
                }
            }
            scope.launch {
                binder.currentDuration().collectLatest {
                    currentDuration.value = it
                }
            }
            scope.launch {
                binder.getCurrentTrack().collectLatest {
                    currentTrack.value = it
                }
            }
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    // Connect to the service on launch
    LaunchedEffect(Unit) {
        val intent = Intent(context, MusicPlayerService::class.java)
        context.bindService(intent, connection, BIND_AUTO_CREATE)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isBound) {
                context.unbindService(connection)
            }
        }
    }

    // Find the index of the selected music
    val initialPageIndex = viewModel.musics.indexOfFirst { it.songId.toString() == musicId }
    LaunchedEffect(initialPageIndex) {
        pagerState.animateScrollToPage(initialPageIndex)
    }

    val currentPage = pagerState.currentPage
    val currentMusic = viewModel.musics.getOrNull(currentPage)

    Column(modifier = modifier.fillMaxSize()) {

        val track by currentTrack.collectAsState()
        val playing by isPlaying.collectAsState()
        val max by maxDuration.collectAsState()
        val current by currentDuration.collectAsState()

        currentMusic?.let {
            Text(
                text = it.songTitle,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            )
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) { page ->
            val selectedMusic = viewModel.musics[page]
            val pageOffset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)

            LaunchedEffect(page) {
                viewModel.loadBitmapIfNeeded(context, page)
            }

            Card(
                modifier = Modifier
                    .graphicsLayer {
                        val scale = lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.absoluteValue)
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(start = 0.4f, stop = 1f, fraction = 1f - pageOffset.absoluteValue)
                        translationX = lerp(start = 0f, stop = 0f, fraction = 1f - pageOffset.absoluteValue)
                    }
            ) {
                AsyncImage(
                    model = selectedMusic.cover,
                    contentDescription = context.getString(Res.string.desc_album_cover_art),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape = MaterialTheme.shapes.medium),
                    contentScale = ContentScale.FillHeight,
                    placeholder = painterResource(Res.drawable.ic_launcher_foreground),
                    error = painterResource(Res.drawable.ic_launcher_foreground)
                )
            }
        }

        Row {
            IconButton(onClick = {
                val intent = Intent(context, MusicPlayerService::class.java)
                context.startService(intent)
                context.bindService(intent, connection, BIND_AUTO_CREATE)
            }) {
                Icon(imageVector = Icons.Default.PlayArrow, null)
            }
            IconButton(onClick = {
                context.stopService(Intent(context, MusicPlayerService::class.java))
                if (isBound) context.unbindService(connection)
            }) {
                Icon(imageVector = Icons.Default.Close, null)
            }
        }

        Spacer(modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = current.div(1000).toString())
                Slider(
                    value = current,
                    onValueChange = {
                        //if (isBound) {
                        //            musicPlayerService?.seekTo(it.toLong())
                        //        })
                    },
                    valueRange = 0f..max,
                )
                Text(text = max.div(1000).toString())
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = {
                    if (isBound) musicPlayerService?.prev()
                }) {
                    Icon(painter = painterResource(Res.drawable.ic_backward), contentDescription = null)
                }


                IconButton(onClick = {
                    if (isBound) musicPlayerService?.playPause()
                }) {
                    Icon(
                        painter = if (playing) painterResource(Res.drawable.ic_pause_circle) else painterResource(Res.drawable.ic_play_circle),
                        contentDescription = null
                    )
                }

                IconButton(onClick = {
                    if (isBound) musicPlayerService?.next()
                }) {
                    Icon(painter = painterResource(Res.drawable.ic_next), contentDescription = null)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerScreenPreview() {
    PlayerScreen("12345", modifier = Modifier)
}