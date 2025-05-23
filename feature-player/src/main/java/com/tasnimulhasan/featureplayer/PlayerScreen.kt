package com.tasnimulhasan.featureplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import com.tasnimulhasan.designsystem.R as Res

@Composable
internal fun PlayerScreen(
    musicId: String,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState { viewModel.audioList.size }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioList = viewModel.audioList

    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val progressString by viewModel.progressString.collectAsStateWithLifecycle()

    // Find the index of the selected music
    val initialPageIndex = viewModel.audioList.indexOfFirst { it.songId.toString() == musicId }
    /*LaunchedEffect(initialPageIndex) {
        pagerState.scrollToPage(initialPageIndex)
    }*/
    LaunchedEffect(initialPageIndex) {
        if (initialPageIndex >= 0) {
            pagerState.scrollToPage(initialPageIndex)
            viewModel.onUiEvents(UIEvents.SelectedAudioChange(initialPageIndex))
            viewModel.onUiEvents(UIEvents.PlayPause)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val currentPageIndex = pagerState.currentPage
        viewModel.onUiEvents(UIEvents.SelectedAudioChange(currentPageIndex))
    }

    val currentPage = pagerState.currentPage
    val currentMusic = viewModel.audioList.getOrNull(currentPage)

    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    LaunchedEffect(currentSelectedAudio) {
        val currentIndex = viewModel.audioList.indexOfFirst { it.songId == currentSelectedAudio.songId }
        if (currentIndex >= 0 && currentIndex != pagerState.currentPage) {
            pagerState.scrollToPage(currentIndex)
        }
    }

    LaunchedEffect(Unit) {
        val currentIndex = viewModel.audioList.indexOfFirst { it.songId.toString() == musicId }
        val currentPlayingIndex = viewModel.currentSelectedAudio.value.let { currentAudio ->
            viewModel.audioList.indexOfFirst { it.songId == currentAudio.songId }
        }
        if (currentIndex >= 0 && currentIndex != currentPlayingIndex) {
            viewModel.onUiEvents(UIEvents.SelectedAudioChange(currentIndex))
            viewModel.onUiEvents(UIEvents.PlayPause)
        } else if (currentIndex >= 0 && !viewModel.isPlaying.value) {
            viewModel.onUiEvents(UIEvents.PlayPause)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) { page ->
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
                    model = currentMusic?.cover,
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

        Spacer(modifier.height(24.dp))

        currentMusic?.let { currentTrack ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                text = currentTrack.songTitle,
                maxLines = 1,
                style = TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                text = currentTrack.artist,
                maxLines = 1,
                style = TextStyle(
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = viewModel.convertLongToReadableDateTime(currentTrack.duration.toLong(), "mm:ss"),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.width(4.dp))

                Slider(
                    modifier = Modifier.weight(4f),
                    value = progress,
                    onValueChange = { viewModel.onUiEvents(UIEvents.SeekTo(it)) },
                    valueRange = 0f..100f
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.toggleTimeDisplay()
                        },
                    text = progressString,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = {
                scope.launch {
                    if (currentPage > 0)
                        pagerState.animateScrollToPage(currentPage - 1)
                    else
                        pagerState.animateScrollToPage(audioList.size - 1)
                }
                viewModel.onUiEvents(UIEvents.SeekToPrevious)
            } ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(Res.drawable.ic_backward),
                    contentDescription = null
                )
            }

            IconButton(onClick = { viewModel.onUiEvents(UIEvents.PlayPause) }) {
                Icon(
                    modifier = Modifier.size(40.dp),
                    painter = if (isPlaying) painterResource(Res.drawable.ic_pause_circle) else painterResource(Res.drawable.ic_play_circle),
                    contentDescription = null
                )
            }

            IconButton(onClick = {
                scope.launch {
                    if (currentPage == viewModel.audioList.size-1)
                        pagerState.animateScrollToPage(0)
                    else
                        pagerState.animateScrollToPage(currentPage + 1)
                }
                viewModel.onUiEvents(UIEvents.SeekToNext)
            }) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(Res.drawable.ic_next),
                    contentDescription = null,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerScreenPreview() {
    PlayerScreen("12345", modifier = Modifier)
}