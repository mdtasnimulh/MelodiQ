package com.tasnimulhasan.featureplayer

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.tasnimulhasan.featureplayer.components.SleepTimerOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import com.tasnimulhasan.designsystem.R as Res

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.PlayerScreen(
    musicId: String,
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit,
    navigateToEqualizerScreen: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val audioList by viewModel.audioList.collectAsStateWithLifecycle()
    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()

    val progressString by viewModel.progressString.collectAsStateWithLifecycle()
    val repeatModeOne by viewModel.repeatModeOne.collectAsStateWithLifecycle()
    val repeatModeAll by viewModel.repeatModeAll.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val repeatModeOff by viewModel.repeatModeOff.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val trackDurationMillis by viewModel.duration.collectAsStateWithLifecycle()

    LaunchedEffect(sortType) {
        viewModel.initializeListIfNeeded()
    }

    val pagerState = rememberPagerState { audioList.size }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val showVolumeBoostDialog = remember { mutableStateOf(false) }
    val volumeGain = remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val maxDragDistance = with(density) { 500.dp.toPx() }
    // Plain, non-animatable drag offset: it's mutated directly (no coroutine hop) on every
    // pointer move, which is what keeps the swipe-to-dismiss gesture smooth. A short-lived
    // Animatable was previously snapTo()'d from inside a freshly launched coroutine on every
    // single drag delta, and that per-event coroutine launch + suspend hop is what caused the
    // dragging lag; settling (spring back / dismiss) below still animates smoothly via `animate {}`.
    var offsetY by remember { mutableFloatStateOf(0f) }
    val thresholdFraction = 0.6f

    val showBottomSheet = remember { mutableStateOf(false) }

    // Sleep timer: local UI state so the sheet can show a live countdown and be reopened
    // to inspect/cancel an already-running timer.
    val sleepTimerRunning = remember { mutableStateOf(false) }
    val sleepTimerEndAtMillis = remember { mutableLongStateOf(0L) }
    val sleepTimerRemainingMillis = remember { mutableLongStateOf(0L) }

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

    fun startSleepTimer(totalDurationMillis: Long) {
        if (sleepTimerRunning.value || totalDurationMillis <= 0L) return
        sleepTimerRemainingMillis.longValue = totalDurationMillis
        sleepTimerEndAtMillis.longValue = System.currentTimeMillis() + totalDurationMillis
        sleepTimerRunning.value = true
    }

    fun cancelSleepTimer() {
        sleepTimerRunning.value = false
        sleepTimerEndAtMillis.longValue = 0L
        sleepTimerRemainingMillis.longValue = 0L
    }

    fun resolveSleepTimerMillis(option: SleepTimerOption): Long = when (option) {
        SleepTimerOption.END_OF_SONG -> {
            val elapsedMillis = (trackDurationMillis * (progress / 100f)).toLong()
            (trackDurationMillis - elapsedMillis).coerceAtLeast(0L)
        }
        SleepTimerOption.MIN_5 -> TimeUnit.MINUTES.toMillis(5)
        SleepTimerOption.MIN_10 -> TimeUnit.MINUTES.toMillis(10)
        SleepTimerOption.MIN_15 -> TimeUnit.MINUTES.toMillis(15)
        SleepTimerOption.MIN_30 -> TimeUnit.MINUTES.toMillis(30)
        SleepTimerOption.MIN_45 -> TimeUnit.MINUTES.toMillis(45)
        SleepTimerOption.HOUR_1 -> TimeUnit.HOURS.toMillis(1)
        SleepTimerOption.HOUR_2 -> TimeUnit.HOURS.toMillis(2)
    }

    // Ticks the remaining time once a second while the timer runs, and fires the same
    // end-of-timer action the original implementation used (pause playback, then close
    // the app) once it reaches zero.
    LaunchedEffect(sleepTimerRunning.value) {
        while (sleepTimerRunning.value) {
            val remaining = sleepTimerEndAtMillis.longValue - System.currentTimeMillis()
            if (remaining <= 0L) {
                sleepTimerRemainingMillis.longValue = 0L
                viewModel.onUiEvents(UIEvents.PlayPause)
                sleepTimerRunning.value = false
                android.os.Process.killProcess(android.os.Process.myPid())
            } else {
                sleepTimerRemainingMillis.longValue = remaining
                delay(1000.milliseconds)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Soft, palette-derived backdrop behind the whole screen for a more elegant,
        // "now playing" feel. Purely decorative - no effect on layout or gestures below.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(lightPaletteColor).copy(alpha = 0.35f),
                            Color(darkPaletteColor).copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, offsetY.toInt()) }
                .graphicsLayer {
                    val progressVal = (offsetY / maxDragDistance).coerceIn(0f, 1f)
                    scaleX = lerp(1f, 0.95f, progressVal)
                    scaleY = lerp(1f, 0.95f, progressVal)
                    alpha = lerp(1f, 0.8f, progressVal)
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            val shouldDismiss = offsetY >= maxDragDistance * thresholdFraction
                            scope.launch {
                                animate(
                                    initialValue = offsetY,
                                    targetValue = if (shouldDismiss) maxDragDistance else 0f,
                                    animationSpec = spring(
                                        dampingRatio = if (shouldDismiss) Spring.DampingRatioNoBouncy else Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) { value, _ -> offsetY = value }
                                if (shouldDismiss) onNavigateUp()
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                animate(
                                    initialValue = offsetY,
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) { value, _ -> offsetY = value }
                            }
                        }
                    ) { change, dragAmount ->
                        // Set the plain state directly - no coroutine launch per drag event.
                        // This is what removes the lag: previously every single pointer-move
                        // callback spun up a new coroutine just to call a suspend snapTo().
                        offsetY = (offsetY + dragAmount).coerceIn(0f, maxDragDistance)
                        change.consume()
                    }
                }
        ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Drag handle affordance - a small pill reinforcing that the screen can be
        // swiped down to dismiss.
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .background(
                    color = Color(darkPaletteColor).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(50)
                )
        )

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(
            visible = sleepTimerRunning.value,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it / 2 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 2 },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(darkPaletteColor).copy(alpha = 0.12f))
                    .clickable { showBottomSheet.value = true }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sleeping in " + formatSleepRemaining(sleepTimerRemainingMillis.longValue),
                    style = TextStyle(
                        color = Color(darkPaletteColor),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

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
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "image-${pageMusic?.songId}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                        .fillMaxSize(),
                    model = pageMusic?.cover,
                    contentDescription = context.getString(Res.string.desc_album_cover_art),
                    contentScale = ContentScale.FillBounds,
                    placeholder = painterResource(Res.drawable.default_cover),
                    error = painterResource(Res.drawable.default_cover)
                )
            }
        }

        Spacer(modifier.height(16.dp))

        currentMusic?.let { currentTrack ->
            Text(
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "title-${currentTrack.songId}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
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
                    currentProgress = normalizedProgress.coerceIn(0f, 1f),
                    isPlaying = isPlaying,
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
                onVolumeBoostClicked = { showVolumeBoostDialog.value = true },
                sleepTimerActive = sleepTimerRunning.value
            )

            if (showBottomSheet.value) {
                SleepTimerBottomSheet(
                    onDismiss = { showBottomSheet.value = false },
                    isTimerRunning = sleepTimerRunning.value,
                    remainingTimeMillis = sleepTimerRemainingMillis.longValue,
                    accentColor = Color(darkPaletteColor),
                    onOptionSelected = { option ->
                        startSleepTimer(resolveSleepTimerMillis(option))
                    },
                    onCancelTimer = { cancelSleepTimer() }
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
}

private fun formatSleepRemaining(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = TimeUnit.SECONDS.toHours(totalSeconds)
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}