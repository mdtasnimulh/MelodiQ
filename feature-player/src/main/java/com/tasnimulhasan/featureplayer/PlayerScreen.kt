package com.tasnimulhasan.featureplayer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.tasnimulhasan.designsystem.theme.LightOrange
import com.tasnimulhasan.designsystem.theme.PeaceOrange
import com.tasnimulhasan.featureplayer.components.CustomButtonGroups
import com.tasnimulhasan.featureplayer.components.CustomWaveProgressBar
import com.tasnimulhasan.featureplayer.components.PlayPauseControlButton
import com.tasnimulhasan.featureplayer.components.SleepTimerBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.random.Random
import com.tasnimulhasan.designsystem.R as Res

@Composable
internal fun PlayerScreen(
    musicId: String,
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit,
    navigateToEqualizerScreen: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState { viewModel.audioList.size }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioList = viewModel.audioList

    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val progressString by viewModel.progressString.collectAsStateWithLifecycle()
    //val progressStringMinutes by viewModel.progressStringMinutes.collectAsStateWithLifecycle()
    //val progressStringSeconds by viewModel.progressStringSeconds.collectAsStateWithLifecycle()
    val repeatModeOne by viewModel.repeatModeOne.collectAsStateWithLifecycle()
    val repeatModeAll by viewModel.repeatModeAll.collectAsStateWithLifecycle()
    val repeatModeOff by viewModel.repeatModeOff.collectAsStateWithLifecycle()

    // Animation and gesture handling
    val density = LocalDensity.current
    val maxDragDistance = with(density) { 500.dp.toPx() } // Max drag distance (adjustable)
    val offsetY = remember { Animatable(0f) }
    val thresholdFraction = 0.6f // 60% threshold for navigation

    val showBottomSheet = remember { mutableStateOf(false) }
    val sleepTimerRunning = remember { mutableStateOf(false) }

    // Find the index of the selected music
    val initialPageIndex = viewModel.audioList.indexOfFirst { it.songId.toString() == musicId }
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

    val darkPaletteColor = remember(currentMusic?.cover) {
        currentMusic?.cover?.let {
            val palette = Palette.from(it).generate()
            palette.vibrantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: LightOrange.toArgb()
        } ?: LightOrange.toArgb()
    }
    val lightPaletteColor = remember(currentMusic?.cover) {
        currentMusic?.cover?.let {
            val palette = Palette.from(it).generate()
            palette.lightVibrantSwatch?.rgb
                ?: palette.lightMutedSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: PeaceOrange.toArgb()
        } ?: PeaceOrange.toArgb()
    }

    fun startSleepTimer(hours: Int, minutes: Int, seconds: Int) {
        if (sleepTimerRunning.value) return // Prevent multiple timers

        val durationMillis = (hours * 3600 + minutes * 60 + seconds) * 1000L
        if (durationMillis > 0) {
            sleepTimerRunning.value = true
            scope.launch {
                delay(durationMillis)
                // Pause playback before killing
                viewModel.onUiEvents(UIEvents.PlayPause)
                // Kill the app
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.value.toInt()) }
            .graphicsLayer {
                val progressVal = (offsetY.value / maxDragDistance).coerceIn(0f, 1f)
                scaleX = lerp(1f, 0.95f, progressVal) // Subtle scale down
                scaleY = lerp(1f, 0.95f, progressVal)
                alpha = lerp(1f, 0.8f, progressVal) // Slight fade
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (offsetY.value >= maxDragDistance * thresholdFraction) {
                                offsetY.animateTo(
                                    maxDragDistance,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium // Faster animation
                                    )
                                )
                                onNavigateUp()
                            } else {
                                offsetY.animateTo(
                                    0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetY.animateTo(
                                0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                ) { change, dragAmount ->
                    scope.launch {
                        val newOffset = (offsetY.value + dragAmount).coerceIn(0f, maxDragDistance)
                        offsetY.snapTo(newOffset)
                    }
                    change.consume()
                }
            }
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp),
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            val pageOffset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)

            LaunchedEffect(page) {
                viewModel.loadBitmapIfNeeded(context, page)
            }

            val pageMusic = viewModel.audioList.getOrNull(page)

            Card(
                modifier = Modifier
                    .padding(horizontal = 15.dp)
                    .graphicsLayer {
                        val scale =
                            lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.absoluteValue)
                        scaleX = scale
                        scaleY = scale
                        alpha =
                            lerp(start = 0.4f, stop = 1f, fraction = 1f - pageOffset.absoluteValue)
                        translationX =
                            lerp(start = 0f, stop = 0f, fraction = 1f - pageOffset.absoluteValue)
                    }
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxSize(),
                    model = pageMusic?.cover,
                    contentDescription = context.getString(Res.string.desc_album_cover_art),
                    contentScale = ContentScale.FillBounds,
                    placeholder = painterResource(Res.drawable.ic_launcher_background),
                    error = painterResource(Res.drawable.ic_launcher_background)
                )
            }
        }

        Spacer(modifier.height(16.dp))

        currentMusic?.let { currentTrack ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .basicMarquee(),
                text = currentTrack.songTitle,
                maxLines = 1,
                style = TextStyle(
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
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
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.toggleTimeDisplay()
                    },
                text = "$progressString / " +viewModel.convertLongToReadableDateTime(
                    currentTrack.duration.toLong(),
                    "mm:ss"
                ),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                val amplitudes = remember { List(60) { Random.nextFloat() } }
                val normalizedProgress = progress / 100f

                CustomWaveProgressBar(
                    amplitudes = amplitudes,
                    currentProgress = normalizedProgress,
                    barColor = Color(darkPaletteColor).copy(alpha = 0.25f),
                    playedColor = Color(darkPaletteColor),
                    onSeek = { normalized ->
                        val seekPosition = normalized * 100f
                        viewModel.onUiEvents(UIEvents.SeekTo(seekPosition))
                    }
                )
            }

            Spacer(modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                PlayPauseControlButton(
                    isPlaying = isPlaying,
                    playButtonColor = Color(darkPaletteColor),

                    onPreviousClick = {
                        scope.launch {
                            if (currentPage > 0)
                                pagerState.animateScrollToPage(currentPage - 1)
                            else
                                pagerState.animateScrollToPage(audioList.size - 1)
                        }
                        viewModel.onUiEvents(UIEvents.SeekToPrevious)
                    },

                    onPlayPauseClick = { viewModel.onUiEvents(UIEvents.PlayPause) },

                    onNextClick = {
                        scope.launch {
                            if (currentPage == viewModel.audioList.size-1)
                                pagerState.animateScrollToPage(0)
                            else
                                pagerState.animateScrollToPage(currentPage + 1)
                        }
                        viewModel.onUiEvents(UIEvents.SeekToNext)
                    },

                    onSeekNextClick = {
                        viewModel.onUiEvents(UIEvents.Forward)
                    },
                    onSeekPreviousClick = {
                        viewModel.onUiEvents(UIEvents.Backward)
                    }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            CustomButtonGroups(
                buttonColor = Color(darkPaletteColor).copy(alpha = 0.05f),
                repeatModeOne = repeatModeOne,
                repeatModeAll = repeatModeAll,
                onRepeatButtonClicked = {
                    if (repeatModeOff && !repeatModeOne && !repeatModeAll) {
                        viewModel.onUiEvents(UIEvents.RepeatOne)
                        Toast.makeText(context, Res.string.msg_repeat_one, Toast.LENGTH_SHORT).show()
                    }
                    else if (!repeatModeOff && repeatModeOne && !repeatModeAll) {
                        viewModel.onUiEvents((UIEvents.RepeatAll))
                        Toast.makeText(context, Res.string.msg_repeat_all, Toast.LENGTH_SHORT).show()
                    }
                    else {
                        viewModel.onUiEvents(UIEvents.RepeatOff)
                        Toast.makeText(context, Res.string.msg_repeat_off, Toast.LENGTH_SHORT).show()
                    }
                },
                onEQButtonClicked = { navigateToEqualizerScreen.invoke() },
                onSleepButtonClicked = { showBottomSheet.value = true },
                onShareButtonClicked = {
                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.type = "audio/*"
                    shareIntent.putExtra(
                        Intent.EXTRA_STREAM, currentTrack.contentUri
                    )
                    context.startActivity(Intent.createChooser(shareIntent, "Sharing ${currentTrack.songTitle}"))
                }
            )

            if (showBottomSheet.value) {
                SleepTimerBottomSheet(
                    onDismiss = { showBottomSheet.value = false },
                    onTimeSet = { hours, minutes, seconds ->
                        startSleepTimer(hours, minutes, seconds)
                    }
                )
            }
        }
    }
}