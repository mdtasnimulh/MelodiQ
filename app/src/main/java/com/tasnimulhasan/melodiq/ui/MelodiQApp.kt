package com.tasnimulhasan.melodiq.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.tasnimulhasan.albums.navigation.AlbumRoute
import com.tasnimulhasan.common.utils.coloredShadow
import com.tasnimulhasan.designsystem.component.MelodiQNavigationBar
import com.tasnimulhasan.designsystem.component.MelodiQNavigationBarItem
import com.tasnimulhasan.designsystem.component.MelodiqTopAppBar
import com.tasnimulhasan.designsystem.icon.MelodiqIcons
import com.tasnimulhasan.featureabout.navigation.AboutRoute
import com.tasnimulhasan.featurefavourite.navigation.FavouriteRoute
import com.tasnimulhasan.featurefeedback.navigation.FeedbackRoute
import com.tasnimulhasan.featureplayer.navigation.PlayerRoute
import com.tasnimulhasan.featurequeue.navigation.QueueRoute
import com.tasnimulhasan.home.navigation.HomeRoute
import com.tasnimulhasan.melodiq.component.CustomDrawer
import com.tasnimulhasan.melodiq.navigation.CustomNavigationItem
import com.tasnimulhasan.melodiq.navigation.MelodiQNavHost
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
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val currentDestination = appState.currentDestination

    val isTopLevelDestination = appState.topLevelDestination.any { destination ->
        currentDestination?.route?.contains(destination.name, true) == true
    }

    val currentTitleRes = when (currentDestination?.route) {
        HomeRoute::class.qualifiedName -> Res.string.app_name
        SongsRoute::class.qualifiedName -> Res.string.title_songs
        AlbumRoute::class.qualifiedName -> Res.string.title_albums
        PlaylistsRoute::class.qualifiedName -> Res.string.title_playlists
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
            Column(
                modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) {
                GetContent(appState = appState)
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
            }
        )
    }
}

private fun NavDestination?.isRouteInHierarchy(route: KClass<*>) =
    this?.hierarchy?.any {
        it.hasRoute(route)
    } ?: false