package com.tasnimulhasan.featureplayer

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.tasnimulhasan.designsystem.theme.CreamRed
import com.tasnimulhasan.designsystem.theme.LightOrange
import com.tasnimulhasan.designsystem.theme.MythicGreen
import com.tasnimulhasan.designsystem.theme.PeaceOrange
import com.tasnimulhasan.designsystem.theme.PeachYellow
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
    val audioList by viewModel.audioList.collectAsStateWithLifecycle()
    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()

    val progressString by viewModel.progressString.collectAsStateWithLifecycle()
    val repeatModeOne by viewModel.repeatModeOne.collectAsStateWithLifecycle()
    val repeatModeAll by viewModel.repeatModeAll.collectAsStateWithLifecycle()
    val repeatModeOff by viewModel.repeatModeOff.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.initializeListIfNeeded() }

    val pagerState = rememberPagerState { audioList.size }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val showVolumeBoostDialog = remember { mutableStateOf(false) }
    val volumeGain = remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val maxDragDistance = with(density) { 500.dp.toPx() }
    val offsetY = remember { Animatable(0f) }
    val thresholdFraction = 0.6f

    val showBottomSheet = remember { mutableStateOf(false) }
    val sleepTimerRunning = remember { mutableStateOf(false) }

    val initialPageIndex = audioList.indexOfFirst { it.songId.toString() == musicId }
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
    val currentMusic = audioList.getOrNull(currentPage)

    LaunchedEffect(currentSelectedAudio) {
        val currentIndex = audioList.indexOfFirst { it.songId == currentSelectedAudio.songId }
        if (currentIndex >= 0 && currentIndex != pagerState.currentPage) {
            pagerState.scrollToPage(currentIndex)
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
        if (sleepTimerRunning.value) return

        val durationMillis = (hours * 3600 + minutes * 60 + seconds) * 1000L
        if (durationMillis > 0) {
            sleepTimerRunning.value = true
            scope.launch {
                delay(durationMillis)
                viewModel.onUiEvents(UIEvents.PlayPause)
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
                scaleX = lerp(1f, 0.95f, progressVal)
                scaleY = lerp(1f, 0.95f, progressVal)
                alpha = lerp(1f, 0.8f, progressVal)
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
                                        stiffness = Spring.StiffnessMedium
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
            LaunchedEffect(page) { viewModel.loadBitmapIfNeeded(context, page) }
            val pageMusic = audioList.getOrNull(page)

            Card(
                modifier = Modifier
                    .padding(horizontal = 15.dp)
                    .graphicsLayer {
                        val scale = lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.absoluteValue)
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(start = 0.4f, stop = 1f, fraction = 1f - pageOffset.absoluteValue)
                        translationX = lerp(start = 0f, stop = 0f, fraction = 1f - pageOffset.absoluteValue)
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
                text = "$progressString / " + viewModel.convertLongToReadableDateTime(
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
                        scope.launch { if (currentPage > 0) pagerState.animateScrollToPage(currentPage - 1) else pagerState.animateScrollToPage(audioList.size - 1) }
                        viewModel.onUiEvents(UIEvents.SeekToPrevious)
                    },
                    onPlayPauseClick = { viewModel.onUiEvents(UIEvents.PlayPause) },
                    onNextClick = {
                        scope.launch { if (currentPage == audioList.size - 1) pagerState.animateScrollToPage(0) else pagerState.animateScrollToPage(currentPage + 1) }
                        viewModel.onUiEvents(UIEvents.SeekToNext)
                    },
                    onSeekNextClick = { viewModel.onUiEvents(UIEvents.Forward) },
                    onSeekPreviousClick = { viewModel.onUiEvents(UIEvents.Backward) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            CustomButtonGroups(
                buttonColor = Color(darkPaletteColor).copy(alpha = 0.05f),
                repeatModeOne = repeatModeOne,
                repeatModeAll = repeatModeAll,
                onRepeatButtonClicked = {
                    if (repeatModeOff && !repeatModeOne && !repeatModeAll) {
                        viewModel.onUiEvents(UIEvents.RepeatOne)
                        Toast.makeText(context, Res.string.msg_repeat_one, Toast.LENGTH_SHORT).show()
                    } else if (!repeatModeOff && repeatModeOne && !repeatModeAll) {
                        viewModel.onUiEvents(UIEvents.RepeatAll)
                        Toast.makeText(context, Res.string.msg_repeat_all, Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.onUiEvents(UIEvents.RepeatOff)
                        Toast.makeText(context, Res.string.msg_repeat_off, Toast.LENGTH_SHORT).show()
                    }
                },
                onEQButtonClicked = { navigateToEqualizerScreen.invoke() },
                onSleepButtonClicked = { showBottomSheet.value = true },
                onShareButtonClicked = {
                    val shareIntent = Intent().also {
                        it.action = Intent.ACTION_SEND
                        it.type = "audio/*"
                        it.putExtra(Intent.EXTRA_STREAM, currentTrack.contentUri)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Sharing ${currentTrack.songTitle}"))
                },
                onVolumeBoostClicked = { showVolumeBoostDialog.value = true }
            )

            if (showBottomSheet.value) {
                SleepTimerBottomSheet(
                    onDismiss = { showBottomSheet.value = false },
                    onTimeSet = { hours, minutes, seconds ->
                        startSleepTimer(hours, minutes, seconds)
                    }
                )
            }

            if (showVolumeBoostDialog.value) {
                LaunchedEffect(showVolumeBoostDialog.value, volume) {
                    volumeGain.floatValue = volume / 200f
                }

                DisposableEffect(showVolumeBoostDialog.value) {
                    val handler = Handler(Looper.getMainLooper())
                    val contentObserver = object : android.database.ContentObserver(handler) {
                        override fun onChange(selfChange: Boolean) {
                            super.onChange(selfChange)
                            if (!viewModel.isAdjustingFromSlider()) {
                                val currentVolume = viewModel.volume.value
                                volumeGain.floatValue = (currentVolume / 200f).coerceIn(0f, 1f)
                                val systemVolumePercent = viewModel.getCurrentVolumePercent()
                                if (currentVolume <= 100 && kotlin.math.abs(currentVolume - systemVolumePercent) > 2) {
                                    volumeGain.floatValue = (systemVolumePercent / 200f).coerceIn(0f, 0.5f)
                                    viewModel.setVolumeWithBoost(systemVolumePercent)
                                } else if (currentVolume > 100 && systemVolumePercent < 100) {
                                    volumeGain.floatValue = (systemVolumePercent / 200f).coerceIn(0f, 0.5f)
                                    viewModel.setVolumeWithBoost(systemVolumePercent)
                                }
                            }
                        }
                    }

                    if (showVolumeBoostDialog.value) {
                        context.contentResolver.registerContentObserver(
                            Settings.System.CONTENT_URI,
                            true,
                            contentObserver
                        )
                    }

                    onDispose {
                        context.contentResolver.unregisterContentObserver(contentObserver)
                    }
                }

                Dialog(onDismissRequest = {
                    showVolumeBoostDialog.value = false
                }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Volume Booster",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            val volumePercent = (volumeGain.floatValue * 200).toInt() // Display 0–200%

                            val sliderColor = when {
                                volumePercent > 150 -> CreamRed
                                volumePercent > 100 -> PeachYellow
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Text(
                                text = "$volumePercent%",
                                fontSize = 16.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Slider(
                                value = volumeGain.floatValue,
                                onValueChange = {
                                    volumeGain.floatValue = it
                                    val newPercent = (it * 200).toInt()
                                    viewModel.setVolumeWithBoost(newPercent, fromSlider = true)
                                },
                                valueRange = 0f..1f,
                                steps = 20,
                                colors = SliderDefaults.colors(
                                    thumbColor = sliderColor,
                                    activeTrackColor = sliderColor,
                                    inactiveTrackColor = Color.LightGray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "OK",
                                    color = MythicGreen,
                                    modifier = Modifier
                                        .clickable {
                                            showVolumeBoostDialog.value = false
                                        }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}