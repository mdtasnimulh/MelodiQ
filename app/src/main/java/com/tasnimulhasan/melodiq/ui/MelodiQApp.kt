package com.tasnimulhasan.melodiq.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.tasnimulhasan.albums.navigation.AlbumRoute
import com.tasnimulhasan.common.service.MelodiqPlayerService
import com.tasnimulhasan.common.utils.coloredShadow
import com.tasnimulhasan.designsystem.component.MelodiQNavigationBar
import com.tasnimulhasan.designsystem.component.MelodiQNavigationBarItem
import com.tasnimulhasan.designsystem.component.MelodiqTopAppBar
import com.tasnimulhasan.designsystem.icon.MelodiqIcons
import com.tasnimulhasan.eqalizer.navigation.EqualizerRoute
import com.tasnimulhasan.featureabout.navigation.AboutRoute
import com.tasnimulhasan.featurefavourite.navigation.FavouriteRoute
import com.tasnimulhasan.featurefeedback.navigation.FeedbackRoute
import com.tasnimulhasan.featureplayer.navigation.PlayerRoute
import com.tasnimulhasan.featurequeue.navigation.QueueRoute
import com.tasnimulhasan.home.navigation.HomeRoute
import com.tasnimulhasan.melodiq.component.CustomDrawer
import com.tasnimulhasan.melodiq.navigation.CustomNavigationItem
import com.tasnimulhasan.melodiq.navigation.MelodiQNavHost
import com.tasnimulhasan.melodiq.ui.miniplayer.MiniPlayer
import com.tasnimulhasan.melodiq.ui.miniplayer.MiniPlayer2
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
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    MmApp(
        appState = appState,
        modifier = modifier,
        onTopAppBarActionClick = { showSettingsDialog = true },
        windowAdaptiveInfo = windowAdaptiveInfo,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MmApp(
    appState: MelodiQAppState,
    modifier: Modifier = Modifier,
    onTopAppBarActionClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val context = LocalContext.current
    val audioList by viewModel.audioList.collectAsStateWithLifecycle()
    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val progressString by viewModel.progressString.collectAsStateWithLifecycle()
    var showPopUpPlayer by remember { mutableStateOf(false) }

    val currentDestination = appState.currentDestination

    val isTopLevelDestination = appState.topLevelDestination.any { destination ->
        currentDestination?.route?.contains(destination.name, true) == true
    }

    val currentTitleRes = when (currentDestination?.route) {
        HomeRoute::class.qualifiedName -> Res.string.app_name
        SongsRoute::class.qualifiedName -> Res.string.title_songs
        AlbumRoute::class.qualifiedName -> Res.string.title_albums
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
                        actionIcon = MelodiqIcons.ActionMore,
                        actionIconsContentDescription = stringResource(id = Res.string.title_settings),
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                        onActionClick = { onTopAppBarActionClick() },
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
                    .background(color = MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) {
                var bitmap by remember { mutableStateOf<Bitmap?>(null) }
                val shouldLoadBitmap = remember(currentSelectedAudio.songId) { true }
                if (audioList.indexOf(currentSelectedAudio) >= 0 && shouldLoadBitmap) {
                    LaunchedEffect(audioList.indexOf(currentSelectedAudio)) {
                        bitmap = viewModel.getAlbumArt(context, currentSelectedAudio.contentUri)
                    }
                }

                GetContent(appState = appState)

                if (currentDestination?.route != PlayerRoute::class.qualifiedName.plus("/{musicId}")) {
                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        visible = context.isServiceRunning(MelodiqPlayerService::class.java) && currentSelectedAudio.songId != 0L && !showPopUpPlayer,
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
                        MiniPlayer(
                            modifier = Modifier,
                            cover = bitmap,
                            onImageClick = { showPopUpPlayer = !showPopUpPlayer }
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
                        MiniPlayer2(
                            modifier = Modifier,
                            cover = bitmap,
                            songTitle = currentSelectedAudio.songTitle,
                            progress = progress,
                            onProgress = { viewModel.onUiEvents(UiEvent.SeekTo(it)) },
                            isPlaying = isPlaying,
                            progressString = "$progressString / " + viewModel.convertLongToReadableDateTime(
                                currentSelectedAudio.duration.toLong(),
                                "mm:ss"
                            ),
                            onMiniPlayerClick = { appState.navigateToPlayer(currentSelectedAudio.songId.toString()) },
                            onPlayPauseClick = { viewModel.onUiEvents(UiEvent.PlayPause) },
                            onNextClick = { viewModel.onUiEvents(UiEvent.SeekToNext) },
                            onPreviousClick = { viewModel.onUiEvents(UiEvent.SeekToPrevious) },
                            onSeekNextClick = { viewModel.onUiEvents(UiEvent.Forward) },
                            onSeekPreviousClick = { viewModel.onUiEvents(UiEvent.Backward) },
                            onImageClick = { showPopUpPlayer = !showPopUpPlayer }
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
                appState.navigateToPlayer(musicId)  // Pass the musicId here
            },
            navigateToEqualizerScreen = {
                appState.navigateToEqualizerScreen()
            },
            navigateBack = {
                appState.navigateBack()
            }
        )
    }
}

@Suppress("DEPRECATION")
fun <T> Context.isServiceRunning(service: Class<T>): Boolean {
    return (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any { it -> it.service.className == service.name }
}

private fun NavDestination?.isRouteInHierarchy(route: KClass<*>) =
    this?.hierarchy?.any {
        it.hasRoute(route)
    } ?: false