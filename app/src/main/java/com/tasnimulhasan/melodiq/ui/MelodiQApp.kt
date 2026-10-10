package com.tasnimulhasan.melodiq.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.tasnimulhasan.entity.enums.MiniPlayerPosition
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.tasnimulhasan.albums.navigation.AlbumRoute
import com.tasnimulhasan.common.utils.coloredShadow
import com.tasnimulhasan.designsystem.component.MelodiQNavigationBar
import com.tasnimulhasan.designsystem.component.MelodiQNavigationBarItem
import com.tasnimulhasan.designsystem.component.MelodiqTopAppBar
import com.tasnimulhasan.designsystem.icon.MelodiqIcons
import com.tasnimulhasan.eqalizer.navigation.EqualizerRoute
import com.tasnimulhasan.featureabout.navigation.AboutRoute
import com.tasnimulhasan.library.navigation.LibraryHubNavRoute
import com.tasnimulhasan.featurefavourite.navigation.FavouriteRoute
import com.tasnimulhasan.featurefeedback.navigation.FeedbackRoute
import com.tasnimulhasan.featureplayer.navigation.PlayerRoute
import com.tasnimulhasan.featurequeue.navigation.QueueRoute
import com.tasnimulhasan.home.navigation.HomeRoute
import com.tasnimulhasan.melodiq.component.CustomDrawer
import com.tasnimulhasan.melodiq.navigation.CustomNavigationItem
import com.tasnimulhasan.melodiq.navigation.MelodiQNavHost
import com.tasnimulhasan.melodiq.ui.miniplayer.MiniPlayer
import com.tasnimulhasan.melodiq.ui.miniplayer.PopUpPlayer
import com.tasnimulhasan.melodiq.ui.viewmodel.MainViewModel
import com.tasnimulhasan.melodiq.ui.viewmodel.UiEvent
import com.tasnimulhasan.playlists.navigation.PlaylistsRoute
import com.tasnimulhasan.settings.navigation.SettingsRoute
import com.tasnimulhasan.songs.navigation.SongsRoute
import kotlin.math.roundToInt
import kotlin.reflect.KClass
import com.tasnimulhasan.designsystem.R as Res

@Composable
fun MelodiQApp(
    appState: MelodiQAppState,
    modifier: Modifier = Modifier,
    openPlayerRequested: Boolean = false,
    onOpenPlayerHandled: () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    MmApp(
        appState = appState,
        modifier = modifier,
        openPlayerRequested = openPlayerRequested,
        onOpenPlayerHandled = onOpenPlayerHandled,
        windowAdaptiveInfo = windowAdaptiveInfo,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MmApp(
    appState: MelodiQAppState,
    modifier: Modifier = Modifier,
    openPlayerRequested: Boolean = false,
    onOpenPlayerHandled: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel(),
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2(),
) {
    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val visualizerStyle by viewModel.visualizerStyle.collectAsStateWithLifecycle()
    var showPopUpPlayer by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val currentDestination = appState.currentDestination

    // Matches the real route, not a substring of its name: package names such as
    // "...library.navigation" would otherwise make every Library sub-screen look top-level.
    val isTopLevelDestination = appState.currentTopLevelDestination != null

    val currentTitleRes = when (currentDestination?.route) {
        HomeRoute::class.qualifiedName -> Res.string.app_name
        SongsRoute::class.qualifiedName -> Res.string.title_songs
        AlbumRoute::class.qualifiedName -> Res.string.title_albums
        LibraryHubNavRoute::class.qualifiedName -> Res.string.title_library
        PlaylistsRoute::class.qualifiedName -> Res.string.title_playlists
        EqualizerRoute::class.qualifiedName -> Res.string.equalizer_title_text
        SettingsRoute::class.qualifiedName -> Res.string.title_settings
        PlayerRoute::class.qualifiedName.plus("/{musicId}") -> Res.string.label_now_playing
        FavouriteRoute::class.qualifiedName -> Res.string.title_favourite
        QueueRoute::class.qualifiedName -> Res.string.title_queue
        AboutRoute::class.qualifiedName -> Res.string.title_about
        FeedbackRoute::class.qualifiedName -> Res.string.title_feedback
        else -> Res.string.app_name
    }

    val navigationIcon = if (isTopLevelDestination) MelodiqIcons.NavigationMenu
    else MelodiqIcons.NavigationBack

    val navigationIconContentDescription = if (isTopLevelDestination) stringResource(id = Res.string.navigation_icon_content_description)
    else stringResource(id = Res.string.navigation_back_content_description)

    var customDrawerState by remember { mutableStateOf(CustomDrawerState.Closed) }
    var selectedNavigationItem by remember { mutableStateOf(CustomNavigationItem.ABOUT) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density

    val screenWidth = remember {
        derivedStateOf { (configuration.screenWidthDp * density).roundToInt() }
    }
    val offsetValue by remember { derivedStateOf { (screenWidth.value / 4.5).dp } }
    val animatedOffset by animateDpAsState(
        targetValue = if (customDrawerState.isOpened()) offsetValue else 0.dp,
        label = "Animated Offset"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (customDrawerState.isOpened()) 0.9f else 1f,
        label = "Animated Scale"
    )
    BackHandler(enabled = customDrawerState.isOpened()) {
        customDrawerState = CustomDrawerState.Closed
    }

    // Playback can outlive the service (app swiped from recents while playing). Whenever
    // audio is playing and this UI is up, make sure the service - and with it the
    // notification and media session - is running again.
    LaunchedEffect(isPlaying) {
        if (isPlaying) viewModel.ensurePlaybackServiceStarted()
    }

    LaunchedEffect(openPlayerRequested, currentSelectedAudio.songId) {
        if (openPlayerRequested && currentSelectedAudio.songId != 0L) {
            appState.navigateToPlayer(currentSelectedAudio.songId.toString())
            onOpenPlayerHandled()
        }
    }

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
    ) {
        CustomDrawer(
            onDrawerCloseClick = { customDrawerState = CustomDrawerState.Closed },
            onAboutClick = { appState.navigateToAbout() },
            onFeedBackClick = { appState.navigateToFeedBack() },
            onFavouriteClick = { appState.navigateToFavourite() },
            onLibraryClick = { appState.navigateToTopLevelDestination(com.tasnimulhasan.melodiq.navigation.TopLevelDestination.LIBRARY) },
            onSettingsClick = { appState.navigateToSettings() }
        )
        Scaffold(
            modifier = modifier
                .offset { IntOffset(x = animatedOffset.roundToPx(), y = 0) }
                .scale(scale = animatedScale)
                .coloredShadow(
                    color = Color.Black,
                    alpha = 0.1f,
                    shadowRadius = 50.dp
                )
                .clickable(enabled = customDrawerState == CustomDrawerState.Opened) {
                    customDrawerState = CustomDrawerState.Closed
                },
            topBar = {
                if (!currentDestination.isRouteInHierarchy(PlayerRoute::class)) {
                    MelodiqTopAppBar(
                        titleRes = currentTitleRes,
                        navigationIcon = navigationIcon,
                        navigationIconContentDescription = navigationIconContentDescription,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Unspecified,
                            navigationIconContentColor = Color.Unspecified,
                            titleContentColor = Color.Unspecified,
                            actionIconContentColor = Color.Unspecified
                        ),
                        onNavigationClick = {
                            if (!isTopLevelDestination) appState.navigateBack()
                            else customDrawerState = customDrawerState.opposite()
                        }
                    )
                }
            },
            bottomBar = {
                if (isTopLevelDestination){
                    MelodiQNavigationBar {
                        appState.topLevelDestination.forEach { destination ->
                            MelodiQNavigationBarItem(
                                selected = currentDestination.isRouteInHierarchy(destination.route),
                                onClick = { appState.navigateToTopLevelDestination(destination) },
                                icon = { Icon(imageVector = destination.unSelectedIcon, contentDescription = null) },
                                selectedIcon = { Icon(imageVector = destination.selectedIcon, contentDescription = null) },
                                label = { Text(stringResource(destination.iconTextId)) },
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Box(
                modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize = it }
                    .background(color = MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) {
                GetContent(appState = appState)

                val miniPosition by viewModel.miniPlayerPosition.collectAsStateWithLifecycle()
                var miniDrag by remember { mutableStateOf(Offset.Zero) }
                val miniAlignment = when (miniPosition) {
                    MiniPlayerPosition.BOTTOM_END -> Alignment.BottomEnd
                    MiniPlayerPosition.BOTTOM_START -> Alignment.BottomStart
                    MiniPlayerPosition.TOP_END -> Alignment.TopEnd
                    MiniPlayerPosition.TOP_START -> Alignment.TopStart
                }

                if (currentDestination?.route != PlayerRoute::class.qualifiedName.plus("/{musicId}")) {
                    AnimatedVisibility(
                        modifier = Modifier
                            .align(miniAlignment)
                            .offset { IntOffset(miniDrag.x.roundToInt(), miniDrag.y.roundToInt()) }
                            .pointerInput(miniPosition, containerSize) {
                                detectDragGestures(
                                    onDrag = { change, amount ->
                                        change.consume()
                                        miniDrag += amount
                                    },
                                    onDragEnd = {
                                        // Snap to whichever corner it was dropped nearest to.
                                        val startsEnd = miniPosition.name.endsWith("END")
                                        val startsBottom = miniPosition.name.startsWith("BOTTOM")
                                        val toEnd = if (startsEnd) miniDrag.x > -containerSize.width / 4f else miniDrag.x > containerSize.width / 4f
                                        val toBottom = if (startsBottom) miniDrag.y > -containerSize.height / 4f else miniDrag.y > containerSize.height / 4f
                                        miniDrag = Offset.Zero
                                        viewModel.setMiniPlayerPosition(
                                            when {
                                                toBottom && toEnd -> MiniPlayerPosition.BOTTOM_END
                                                toBottom -> MiniPlayerPosition.BOTTOM_START
                                                toEnd -> MiniPlayerPosition.TOP_END
                                                else -> MiniPlayerPosition.TOP_START
                                            }
                                        )
                                    },
                                    onDragCancel = { miniDrag = Offset.Zero },
                                )
                            },
                        visible = currentSelectedAudio.songId != 0L && !showPopUpPlayer,
                        enter = scaleIn(
                            animationSpec = tween(durationMillis = 500),
                            transformOrigin = TransformOrigin(
                                pivotFractionX = 1f,
                                pivotFractionY = 1f
                            )
                        ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                        exit = scaleOut(
                            animationSpec = tween(durationMillis = 500),
                            transformOrigin = TransformOrigin(
                                pivotFractionX = 1f,
                                pivotFractionY = 1f
                            )
                        ) + fadeOut(animationSpec = tween(durationMillis = 500))
                    ) {
                        // Collected here (not at the top of MmApp) so ~20fps visualizer
                        // updates only recompose this player, not the whole scaffold.
                        val visualizerBars by viewModel.visualizerBars.collectAsStateWithLifecycle()
                        MiniPlayer(
                            modifier = Modifier,
                            songId = currentSelectedAudio.songId,
                            contentUri = currentSelectedAudio.contentUri,
                            albumId = currentSelectedAudio.albumId,
                            onImageClick = { showPopUpPlayer = !showPopUpPlayer },
                            visualizerStyle = visualizerStyle,
                            visualizerBars = visualizerBars,
                        )
                    }

                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        visible = showPopUpPlayer,
                        enter = scaleIn(
                            animationSpec = tween(durationMillis = 500),
                            transformOrigin = TransformOrigin(
                                pivotFractionX = 1f,
                                pivotFractionY = 1f
                            )
                        ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                        exit = scaleOut(
                            animationSpec = tween(durationMillis = 500),
                            transformOrigin = TransformOrigin(
                                pivotFractionX = 1f,
                                pivotFractionY = 1f
                            )
                        ) + fadeOut(animationSpec = tween(durationMillis = 500))
                    ) {
                        // Collected here, not at the top of MmApp, so the 500ms progress tick
                        // only recomposes the popup instead of the whole app scaffold.
                        val progress by viewModel.progress.collectAsStateWithLifecycle()
                        val progressString by viewModel.progressString.collectAsStateWithLifecycle()
                        val visualizerBars by viewModel.visualizerBars.collectAsStateWithLifecycle()
                        PopUpPlayer(
                            modifier = Modifier,
                            songId = currentSelectedAudio.songId,
                            contentUri = currentSelectedAudio.contentUri,
                            albumId = currentSelectedAudio.albumId,
                            songTitle = currentSelectedAudio.songTitle,
                            progress = progress,
                            onProgress = { seekPosition -> viewModel.onUiEvents(UiEvent.SeekTo(seekPosition)) },
                            isPlaying = isPlaying,
                            progressString = "$progressString / " + viewModel.convertLongToReadableDateTime(
                                // duration is "" on the placeholder song that is briefly shown while the popup
                                // plays its exit animation after "close player" - toLong() crashed on it.
                                currentSelectedAudio.duration.toLongOrNull() ?: 0L,
                                "mm:ss"
                            ),
                            onMiniPlayerClick = { appState.navigateToPlayer(currentSelectedAudio.songId.toString()) },
                            onPlayPauseClick = { viewModel.onUiEvents(UiEvent.PlayPause) },
                            onNextClick = { viewModel.onUiEvents(UiEvent.SeekToNext) },
                            onPreviousClick = { viewModel.onUiEvents(UiEvent.SeekToPrevious) },
                            onSeekNextClick = { viewModel.onUiEvents(UiEvent.Forward) },
                            onSeekPreviousClick = { viewModel.onUiEvents(UiEvent.Backward) },
                            onImageClick = { showPopUpPlayer = !showPopUpPlayer },
                            onCloseClick = {
                                showPopUpPlayer = false
                                viewModel.onUiEvents(UiEvent.StopPlayback)
                            },
                            visualizerStyle = visualizerStyle,
                            visualizerBars = visualizerBars,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GetContent(appState: MelodiQAppState) {
    Box(modifier = Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
        MelodiQNavHost(
            appState = appState,
            navigateToPlayer = { musicId ->
                appState.navigateToPlayer(musicId)
            },
            navigateToEqualizerScreen = {
                appState.navigateToEqualizerScreen()
            },
            onPlaylistClicked = { playlistId, playlistName ->
                appState.navigateToPlaylistDetails(playlistId, playlistName)
            },
            navigateToAlbumDetails = { albumId ->
                appState.navigateToAlbumDetails(albumId)
            },
            navigateBack = {
                appState.navigateBack()
            }
        )
    }
}

private fun NavDestination?.isRouteInHierarchy(route: KClass<*>) =
    this?.hierarchy?.any {
        it.hasRoute(route)
    } ?: false