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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.tasnimulhasan.entity.enums.CoverArtStyle
import com.tasnimulhasan.entity.enums.VisualizerStyle
import com.tasnimulhasan.featureplayer.components.CustomWaveProgressBar
import com.tasnimulhasan.featureplayer.components.ExpressiveTransportControls
import com.tasnimulhasan.featureplayer.components.FavoriteButton
import com.tasnimulhasan.featureplayer.components.PlayerAction
import com.tasnimulhasan.featureplayer.components.PlayerActionChips
import com.tasnimulhasan.featureplayer.components.PlayerTopBar
import com.tasnimulhasan.featureplayer.components.SleepTimerBottomSheet
import com.tasnimulhasan.featureplayer.components.SleepTimerOption
import com.tasnimulhasan.featureplayer.components.expressiveEntrance
import com.tasnimulhasan.ui.image.AlbumArt
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.random.Random
import com.tasnimulhasan.designsystem.R as Res

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.PlayerScreen(
    musicId: String,
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit,
    navigateToEqualizerScreen: () -> Unit,
    navigateToLyrics: () -> Unit,
    navigateToQueue: () -> Unit,
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

    // Start the pager on the song this screen was opened for. Starting at page 0 (the
    // default) and only scrolling later is what made the old code think the user had swiped
    // to the FIRST song and announce it as a track change.
    val initialPage = remember {
        val targetId = musicId.toLongOrNull()
        audioList.indexOfFirst { it.songId == targetId }.takeIf { it >= 0 }
            ?: audioList.indexOfFirst { it.songId == currentSelectedAudio.songId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { audioList.size }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val showVolumeBoostDialog = remember { mutableStateOf(false) }
    val volumeGain = remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val maxDragDistance = with(density) { 500.dp.toPx() }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val thresholdFraction = 0.6f

    val showBottomSheet = remember { mutableStateOf(false) }

    val sleepTimerRunning by viewModel.sleepTimerActive.collectAsStateWithLifecycle()
    val sleepTimerRemainingMillis by viewModel.sleepTimerRemainingMillis.collectAsStateWithLifecycle()

    // Pager -> ViewModel: ONLY when the user physically drags the pager. Any other page
    // change (initial position, programmatic sync, next/previous buttons) is a reflection
    // of the player's state and must never change what is playing or start playback.
    val latestAudioList by rememberUpdatedState(audioList)
    val latestCurrentSongId by rememberUpdatedState(currentSelectedAudio.songId)
    var userDraggedPager by remember { mutableStateOf(false) }

    val currentPage = pagerState.currentPage
    val currentMusic = audioList.getOrNull(currentPage)

    val paletteThumbnail = com.tasnimulhasan.ui.image.rememberPaletteThumbnail(
        songId = currentMusic?.songId ?: 0L,
        contentUri = currentMusic?.contentUri ?: android.net.Uri.EMPTY,
    )
    val darkPaletteColor = remember(paletteThumbnail) {
        paletteThumbnail?.let {
            val palette = Palette.from(it).generate()
            palette.vibrantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: LightOrange.toArgb()
        } ?: LightOrange.toArgb()
    }
    val lightPaletteColor = remember(paletteThumbnail) {
        paletteThumbnail?.let {
            val palette = Palette.from(it).generate()
            palette.lightVibrantSwatch?.rgb
                ?: palette.lightMutedSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: PeaceOrange.toArgb()
        } ?: PeaceOrange.toArgb()
    }

    // Cover art presentation + how the visualizer should fit it. The ring visualizer only
    // makes sense around round art (Settings enforces that pairing; this just guards the
    // render so a stale saved combo can never draw a ring around a rectangle).
    val coverArtStyle by viewModel.coverArtStyle.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val visualizerStyle by viewModel.visualizerStyle.collectAsStateWithLifecycle()
    val isFull = coverArtStyle == CoverArtStyle.FULL
    val isCircle = coverArtStyle == CoverArtStyle.CIRCLE
    val ringVisualizer = isCircle && visualizerStyle == VisualizerStyle.CIRCULAR
    // In Full mode the controls sit on a dark translucent panel, where the (dark) palette
    // color would vanish - so the accent flips to white with dark content on top of it.
    val accent = if (isFull) Color.White else Color(darkPaletteColor)
    val onAccent = if (isFull) Color.Black.copy(alpha = 0.87f) else Color.White
    val primaryText = if (isFull) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryText = if (isFull) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant

    // ViewModel -> Pager: keep the pager visually on the current song (next/previous
    // buttons, auto-advance, notification controls). Never triggers playback changes.
    LaunchedEffect(currentSelectedAudio.songId, audioList) {
        val targetIndex = audioList.indexOfFirst { it.songId == currentSelectedAudio.songId }
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    LaunchedEffect(pagerState) {
        pagerState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) userDraggedPager = true
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .drop(1)
            .collect { page ->
                if (!userDraggedPager) return@collect
                userDraggedPager = false
                val settledSongId = latestAudioList.getOrNull(page)?.songId ?: return@collect
                if (settledSongId != latestCurrentSongId) {
                    viewModel.onUiEvents(UIEvents.SelectedAudioChange(page))
                }
            }
    }

    fun resolveSleepTimerMillis(option: SleepTimerOption): Long = when (option) {
        SleepTimerOption.END_OF_SONG -> 0L
        SleepTimerOption.MIN_5 -> TimeUnit.MINUTES.toMillis(5)
        SleepTimerOption.MIN_10 -> TimeUnit.MINUTES.toMillis(10)
        SleepTimerOption.MIN_15 -> TimeUnit.MINUTES.toMillis(15)
        SleepTimerOption.MIN_30 -> TimeUnit.MINUTES.toMillis(30)
        SleepTimerOption.MIN_45 -> TimeUnit.MINUTES.toMillis(45)
        SleepTimerOption.HOUR_1 -> TimeUnit.HOURS.toMillis(1)
        SleepTimerOption.HOUR_2 -> TimeUnit.HOURS.toMillis(2)
    }

    // Circle art slowly spins while playing and holds its angle when paused. The angle is only
    // READ inside graphicsLayer {} blocks, so ticking it every frame never recomposes the
    // screen - it just redraws the layer.
    var coverAngle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying, isCircle) {
        if (isPlaying && isCircle) {
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                coverAngle = (coverAngle + (now - last) / 1_000_000_000f * 12f) % 360f
                last = now
            }
        }
    }
    // Art eases slightly smaller when paused and springs back on play.
    val playScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "artPlayScale"
    )

    val artPager: @Composable (Modifier) -> Unit = { pagerModifier ->
        HorizontalPager(
            modifier = pagerModifier,
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            val pageOffset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
            val pageMusic = audioList.getOrNull(page)
            val artModel = AlbumArt(
                songId = pageMusic?.songId ?: 0L,
                contentUri = pageMusic?.contentUri ?: android.net.Uri.EMPTY,
                albumId = pageMusic?.albumId ?: 0L,
            )
            val sharedImageModifier = Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "image-${pageMusic?.songId}"),
                animatedVisibilityScope = animatedVisibilityScope,
            )

            when (coverArtStyle) {
                CoverArtStyle.HALF -> Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 15.dp)
                        .graphicsLayer {
                            val scale = lerp(0.85f, 1f, 1f - pageOffset.absoluteValue) * playScale
                            scaleX = scale
                            scaleY = scale
                            alpha = lerp(0.4f, 1f, 1f - pageOffset.absoluteValue)
                        }
                ) {
                    AsyncImage(
                        modifier = sharedImageModifier.fillMaxSize(),
                        model = artModel,
                        contentDescription = stringResource(Res.string.desc_album_cover_art),
                        contentScale = ContentScale.FillBounds,
                        placeholder = painterResource(Res.drawable.default_cover),
                        error = painterResource(Res.drawable.default_cover)
                    )
                }

                CoverArtStyle.CIRCLE -> BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .graphicsLayer {
                            alpha = lerp(0.35f, 1f, 1f - pageOffset.absoluteValue)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val side = if (maxWidth < maxHeight) maxWidth else maxHeight
                    Box(modifier = Modifier.size(side), contentAlignment = Alignment.Center) {
                        // The ring only needs to exist on the page actually being listened
                        // to - neighbouring pages in the pager don't have live audio data.
                        if (ringVisualizer && page == pagerState.currentPage) {
                            PlayerVisualizerRing(viewModel, Modifier.fillMaxSize())
                        }
                        AsyncImage(
                            modifier = sharedImageModifier
                                .fillMaxSize(if (ringVisualizer) 0.78f else 0.94f)
                                .graphicsLayer {
                                    rotationZ = coverAngle
                                    scaleX = playScale
                                    scaleY = playScale
                                }
                                .clip(CircleShape)
                                .border(2.dp, Color(darkPaletteColor).copy(alpha = 0.35f), CircleShape),
                            model = artModel,
                            contentDescription = stringResource(Res.string.desc_album_cover_art),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(Res.drawable.default_cover),
                            error = painterResource(Res.drawable.default_cover)
                        )
                    }
                }

                CoverArtStyle.FULL -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = lerp(0.3f, 1f, 1f - pageOffset.absoluteValue) }
                ) {
                    AsyncImage(
                        modifier = sharedImageModifier.fillMaxSize(),
                        model = artModel,
                        contentDescription = stringResource(Res.string.desc_album_cover_art),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(Res.drawable.default_cover),
                        error = painterResource(Res.drawable.default_cover)
                    )
                }
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

        // Wrapper carries the drag-to-dismiss offset/scale/gesture so that in Full mode the
        // background artwork moves together with the controls instead of staying behind.
        Box(
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
            if (isFull) {
                // Full screen: swipeable artwork fills the whole screen behind everything,
                // with a scrim so the title and controls on top stay readable.
                artPager(Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Black.copy(alpha = 0.35f),
                                    0.22f to Color.Transparent,
                                    0.42f to Color.Transparent,
                                    0.72f to Color.Black.copy(alpha = 0.78f),
                                    1f to Color.Black.copy(alpha = 0.94f),
                                )
                            )
                        )
                )
            }
        Column(modifier = Modifier.fillMaxSize()) {
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

            Spacer(modifier = Modifier.height(8.dp))

            PlayerTopBar(
                albumName = currentMusic?.album.orEmpty(),
                onClose = onNavigateUp,
                onQueue = { navigateToQueue.invoke() },
                onDark = isFull,
                accent = Color(darkPaletteColor),
                modifier = Modifier.expressiveEntrance(0),
            )

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(
                visible = sleepTimerRunning,
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
                        text = "Sleeping in " + formatSleepRemaining(sleepTimerRemainingMillis),
                        style = TextStyle(
                            color = Color(darkPaletteColor),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Album art gets first claim on any extra vertical space: fixed-size text/controls
            // below keep their natural height and the art expands to fill what's left (heightIn
            // keeps it from collapsing on short screens). In Full mode the art is the
            // background layer instead, so this slot is just flexible empty space.
            if (isFull) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                artPager(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 220.dp)
                )
            }

            PlayerVisualizerStrip(viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            currentMusic?.let { currentTrack ->
                val panelModifier = if (isFull) {
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.72f))
                            )
                        )
                        .padding(top = 24.dp, bottom = 20.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                }

                Column(modifier = panelModifier) {
                    // Title / artist on the left, favorite on the right.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .expressiveEntrance(1),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                modifier = Modifier
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = "title-${currentTrack.songId}"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                    )
                                    .fillMaxWidth()
                                    .basicMarquee(),
                                text = currentTrack.songTitle,
                                maxLines = 1,
                                style = TextStyle(
                                    color = primaryText,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentTrack.artist,
                                maxLines = 1,
                                style = TextStyle(
                                    color = secondaryText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                ),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        FavoriteButton(
                            isFavorite = favorites.contains(currentTrack.songId),
                            onClick = { viewModel.toggleFavorite(currentTrack.songId) },
                            onDark = isFull,
                            accent = Color(darkPaletteColor),
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(horizontal = 8.dp)
                            .expressiveEntrance(2),
                        contentAlignment = Alignment.Center
                    ) {
                        val amplitudes = remember { List(60) { Random.nextFloat() } }
                        val normalizedProgress = progress / 100f

                        CustomWaveProgressBar(
                            amplitudes = amplitudes,
                            currentProgress = normalizedProgress.coerceIn(0f, 1f),
                            isPlaying = isPlaying,
                            barColor = if (isFull) Color.White.copy(alpha = 0.3f) else Color(darkPaletteColor).copy(alpha = 0.25f),
                            playedColor = accent,
                            onSeek = { normalized ->
                                val seekPosition = normalized * 100f
                                viewModel.onUiEvents(UIEvents.SeekTo(seekPosition))
                            }
                        )
                    }

                    // Elapsed on the left, total on the right (tap to toggle the display mode).
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .expressiveEntrance(2),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            modifier = Modifier.clickable { viewModel.toggleTimeDisplay() },
                            text = progressString,
                            style = TextStyle(color = secondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = viewModel.convertLongToReadableDateTime(currentTrack.duration.toLong(), "mm:ss"),
                            style = TextStyle(color = secondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ExpressiveTransportControls(
                        isPlaying = isPlaying,
                        accent = accent,
                        onAccent = onAccent,
                        onDark = isFull,
                        modifier = Modifier.expressiveEntrance(3),
                        onPrevious = {
                            scope.launch { if (currentPage > 0) pagerState.animateScrollToPage(currentPage - 1) else pagerState.animateScrollToPage(audioList.size - 1) }
                            viewModel.onUiEvents(UIEvents.SeekToPrevious)
                        },
                        onPlayPause = { viewModel.onUiEvents(UIEvents.PlayPause) },
                        onNext = {
                            scope.launch { if (currentPage == audioList.size - 1) pagerState.animateScrollToPage(0) else pagerState.animateScrollToPage(currentPage + 1) }
                            viewModel.onUiEvents(UIEvents.SeekToNext)
                        },
                        onSeekForward = { viewModel.onUiEvents(UIEvents.Forward) },
                        onSeekBack = { viewModel.onUiEvents(UIEvents.Backward) },
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PlayerActionChips(
                        accent = accent,
                        onAccent = onAccent,
                        onDark = isFull,
                        modifier = Modifier.expressiveEntrance(4),
                        actions = listOf(
                            PlayerAction(
                                icon = when {
                                    repeatModeAll -> Icons.Default.AllInclusive
                                    repeatModeOne -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                label = when {
                                    repeatModeAll -> "Repeat all"
                                    repeatModeOne -> "Repeat one"
                                    else -> "Repeat"
                                },
                                active = repeatModeAll || repeatModeOne,
                                onClick = {
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
                            ),
                            PlayerAction(
                                icon = Icons.AutoMirrored.Filled.Article,
                                label = "Lyrics",
                                onClick = { navigateToLyrics.invoke() },
                            ),
                            PlayerAction(
                                icon = Icons.Default.Timer,
                                label = if (sleepTimerRunning) formatSleepRemaining(sleepTimerRemainingMillis) else "Sleep timer",
                                active = sleepTimerRunning,
                                onClick = { showBottomSheet.value = true },
                            ),
                            PlayerAction(
                                icon = Icons.Default.GraphicEq,
                                label = "Equalizer",
                                onClick = { navigateToEqualizerScreen.invoke() },
                            ),
                            PlayerAction(
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                label = "Volume boost",
                                onClick = { showVolumeBoostDialog.value = true },
                            ),
                            PlayerAction(
                                icon = Icons.Default.Share,
                                label = "Share",
                                onClick = {
                                    val shareIntent = Intent().also {
                                        it.action = Intent.ACTION_SEND
                                        it.type = "audio/*"
                                        it.putExtra(Intent.EXTRA_STREAM, currentTrack.contentUri)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Sharing ${currentTrack.songTitle}"))
                                },
                            ),
                        ),
                    )
                }

                if (showBottomSheet.value) {
                    SleepTimerBottomSheet(
                        onDismiss = { showBottomSheet.value = false },
                        isTimerRunning = sleepTimerRunning,
                        remainingTimeMillis = sleepTimerRemainingMillis,
                        accentColor = Color(darkPaletteColor),
                        onOptionSelected = { option, fadeOutSeconds ->
                            if (option == SleepTimerOption.END_OF_SONG) {
                                viewModel.startEndOfSongSleepTimer(fadeOutSeconds)
                            } else {
                                viewModel.startSleepTimer(resolveSleepTimerMillis(option), fadeOutSeconds)
                            }
                        },
                        onCustomTimeSet = { h, m, s, fadeOutSeconds ->
                            viewModel.startSleepTimer((h * 3600L + m * 60L + s) * 1000L, fadeOutSeconds)
                        },
                        onCancelTimer = { viewModel.cancelSleepTimer() }
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
                                    color = MaterialTheme.colorScheme.onSurface,
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** Collects the ~20fps bar data in its own scope, so only this strip recomposes per frame -
 * not the whole player screen around it. Renders nothing when the style is OFF. */
@Composable
private fun PlayerVisualizerStrip(viewModel: PlayerViewModel) {
    val style by viewModel.visualizerStyle.collectAsStateWithLifecycle()
    // OFF draws nothing; CIRCULAR is drawn as a ring around the (round) cover art instead of
    // as a strip, so the strip stays out of its way.
    if (style == com.tasnimulhasan.entity.enums.VisualizerStyle.OFF ||
        style == com.tasnimulhasan.entity.enums.VisualizerStyle.CIRCULAR
    ) return
    val bars by viewModel.visualizerBars.collectAsStateWithLifecycle()
    com.tasnimulhasan.ui.visualizer.AudioVisualizer(
        bars = bars,
        style = style,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

/** The circular visualizer drawn around round cover art. Collects the bar data itself so only
 * the ring recomposes per frame. */
@Composable
private fun PlayerVisualizerRing(viewModel: PlayerViewModel, modifier: Modifier) {
    val bars by viewModel.visualizerBars.collectAsStateWithLifecycle()
    com.tasnimulhasan.ui.visualizer.AudioVisualizer(
        bars = bars,
        style = com.tasnimulhasan.entity.enums.VisualizerStyle.CIRCULAR,
        modifier = modifier,
    )
}
