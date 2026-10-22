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
import com.tasnimulhasan.designsystem.component.Icon
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.tasnimulhasan.library.navigation.SearchNavRoute
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
import com.tasnimulhasan.designsystem.component.FloatingNavItem
import com.tasnimulhasan.designsystem.component.LocalFloatingBarPadding
import com.tasnimulhasan.designsystem.component.FloatingNavAction
import com.tasnimulhasan.designsystem.component.MelodiqFloatingNavBar
import androidx.compose.material.icons.filled.Headphones
import com.tasnimulhasan.featureaudiobook.navigation.AudioBookDetailsRoute
import com.tasnimulhasan.featureaudiobook.navigation.BookReaderRoute
import com.tasnimulhasan.featureaudiobook.navigation.AudioBookRoute
import com.tasnimulhasan.designsystem.component.MelodiqSearchTopBar
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
import com.tasnimulhasan.designsystem.theme.glassBackdrop
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
    showNavLabels: Boolean = true,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    MmApp(
        showNavLabels = showNavLabels,
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
    showNavLabels: Boolean = true,
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
    val onAudioBook = currentDestination?.route == AudioBookRoute::class.qualifiedName
    val isTopLevelDestination = appState.currentTopLevelDestination != null || onAudioBook

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
        AudioBookRoute::class.qualifiedName -> Res.string.title_audio_book
        AudioBookDetailsRoute::class.qualifiedName.plus("/{bookId}") -> Res.string.title_book_details
        else -> Res.string.app_name
    }

    val navigationIcon = if (isTopLevelDestination) MelodiqIcons.NavigationMenu
    else MelodiqIcons.NavigationBack

    val navigationIconContentDescription = if (isTopLevelDestination) stringResource(id = Res.string.navigation_icon_content_description)
    else stringResource(id = Res.string.navigation_back_content_description)

    var customDrawerState by remember { mutableStateOf(CustomDrawerState.Closed) }

    // iOS-style: the floating bar shrinks to icons while the content scrolls down and comes
    // back when it scrolls up. Only observes scrolling (never consumes it), accumulating the
    // distance so slow scrolls count too and a direction change starts over.
    var barMinimized by remember { mutableStateOf(false) }
    val scrollWatcher = remember {
        object : NestedScrollConnection {
            private var travelled = 0f
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy == 0f) return Offset.Zero
                if ((dy > 0f) != (travelled > 0f)) travelled = 0f
                travelled += dy
                if (travelled < -48f) barMinimized = true
                else if (travelled > 48f) barMinimized = false
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(currentDestination?.route) { barMinimized = false }
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

    val glassTokens = com.tasnimulhasan.designsystem.theme.LocalGlass.current
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
    ) {
        // Only compose the drawer while it is open or still animating, so it can never show
        // through a transparent (Glass) page.
        if (customDrawerState.isOpened() || animatedScale < 0.999f) CustomDrawer(
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
                // Glass screens are transparent: the page carries its own opaque backdrop so the
                // drawer behind it never shows through.
                .then(
                    if (glassTokens != null) Modifier.glassBackdrop(glassTokens)
                    else Modifier
                )
                .clickable(enabled = customDrawerState == CustomDrawerState.Opened) {
                    customDrawerState = CustomDrawerState.Closed
                },
            topBar = {
                // The Audio Book screens draw their own header, so the app bar is hidden there.
                val hideAppBar = currentDestination.isRouteInHierarchy(PlayerRoute::class) ||
                    currentDestination.isRouteInHierarchy(AudioBookRoute::class) ||
                    currentDestination.isRouteInHierarchy(AudioBookDetailsRoute::class) ||
                    currentDestination.isRouteInHierarchy(BookReaderRoute::class)
                if (!hideAppBar) {
                    val onSearch = currentDestination.isRouteInHierarchy(SearchNavRoute::class)
                    val onSettings = currentDestination.isRouteInHierarchy(SettingsRoute::class)
                    // No screen title any more: the search pill takes its place. currentTitleRes
                    // above is intentionally kept (unused) in case titles come back.
                    MelodiqSearchTopBar(
                        navigationIcon = navigationIcon,
                        navigationIconContentDescription = navigationIconContentDescription,
                        onNavigationClick = {
                            if (!isTopLevelDestination) appState.navigateBack()
                            else customDrawerState = customDrawerState.opposite()
                        },
                        onSearchClick = { appState.navigateToSearch() },
                        onSettingsClick = { appState.navigateToSettings() },
                        showSearch = !onSearch,
                        showSettings = !onSearch && !onSettings,
                    )
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
                    .nestedScroll(scrollWatcher)
                    .background(color = MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) {
                CompositionLocalProvider(
                    LocalFloatingBarPadding provides if (isTopLevelDestination) FloatingBarSpace else 0.dp
                ) {
                    GetContent(appState = appState)
                }

                val barItems = remember {
                    appState.topLevelDestination.map {
                        FloatingNavItem(label = "", icon = it.unSelectedIcon, selectedIcon = it.selectedIcon)
                    }
                }
                val labels = appState.topLevelDestination.map { stringResource(it.iconTextId) }
                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    visible = isTopLevelDestination && !currentDestination.isRouteInHierarchy(AudioBookRoute::class),
                    enter = fadeIn(tween(250)) + androidx.compose.animation.slideInVertically(tween(300)) { it / 2 },
                    exit = fadeOut(tween(200)) + androidx.compose.animation.slideOutVertically(tween(200)) { it / 2 },
                ) {
                    MelodiqFloatingNavBar(
                        modifier = Modifier.padding(bottom = 12.dp),
                        items = barItems.mapIndexed { i, item -> item.copy(label = labels[i]) },
                        selectedIndex = if (onAudioBook) -1
                        else appState.topLevelDestination.indexOfFirst { currentDestination.isRouteInHierarchy(it.route) }.coerceAtLeast(0),
                        minimized = barMinimized,
                        showLabels = showNavLabels,
                        action = FloatingNavAction(
                            label = stringResource(Res.string.title_audio_book),
                            icon = androidx.compose.material.icons.Icons.Filled.Headphones,
                            selected = onAudioBook,
                            onClick = { appState.navigateToAudioBook() },
                        ),
                        onSelect = { index -> appState.navigateToTopLevelDestination(appState.topLevelDestination[index]) },
                    )
                }

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
                            .padding(bottom = if (isTopLevelDestination && miniAlignment.isBottom()) FloatingBarSpace else 0.dp)
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
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (isTopLevelDestination) FloatingBarSpace else 0.dp),
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

/** Height reserved for the floating bar: 68dp bar + 12dp gap + a little breathing room. */
private val FloatingBarSpace = 92.dp

private fun Alignment.isBottom() = this == Alignment.BottomEnd || this == Alignment.BottomStart
