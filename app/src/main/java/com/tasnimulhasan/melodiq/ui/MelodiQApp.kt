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
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.navigation.NavDestination.Companion.hierarchy
import com.tasnimulhasan.designsystem.component.MelodiqTopAppBar
import com.tasnimulhasan.designsystem.component.MmNavigationSuiteScaffold
import com.tasnimulhasan.designsystem.icon.MelodiqIcons
import com.tasnimulhasan.melodiq.component.CustomDrawer
import com.tasnimulhasan.melodiq.component.CustomDrawerState
import com.tasnimulhasan.melodiq.component.CustomNavigationItem
import com.tasnimulhasan.melodiq.component.coloredShadow
import com.tasnimulhasan.melodiq.component.isOpened
import com.tasnimulhasan.melodiq.component.opposite
import com.tasnimulhasan.melodiq.navigation.MelodiQNavHost
import com.tasnimulhasan.melodiq.navigation.TopLevelDestination
import com.tasnimulhasan.ui.NavRoutes.ABOUT_ROUTE
import com.tasnimulhasan.ui.NavRoutes.ALBUMS_ROUTE
import com.tasnimulhasan.ui.NavRoutes.FAVOURITE_ROUTE
import com.tasnimulhasan.ui.NavRoutes.FEEDBACK_ROUTE
import com.tasnimulhasan.ui.NavRoutes.HOME_ROUTE
import com.tasnimulhasan.ui.NavRoutes.PLAYER_ROUTE
import com.tasnimulhasan.ui.NavRoutes.PLAYLISTS_ROUTE
import com.tasnimulhasan.ui.NavRoutes.QUEUE_ROUTE
import com.tasnimulhasan.ui.NavRoutes.SETTINGS_ROUTE
import com.tasnimulhasan.ui.NavRoutes.SONGS_ROUTE
import kotlin.math.roundToInt
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
        HOME_ROUTE -> Res.string.app_name
        SONGS_ROUTE -> Res.string.title_songs
        ALBUMS_ROUTE -> Res.string.title_albums
        PLAYLISTS_ROUTE -> Res.string.title_playlists
        SETTINGS_ROUTE -> Res.string.title_settings
        PLAYER_ROUTE -> Res.string.label_now_playing
        FAVOURITE_ROUTE -> Res.string.title_favourite
        QUEUE_ROUTE -> Res.string.title_queue
        ABOUT_ROUTE -> Res.string.title_about
        FEEDBACK_ROUTE -> Res.string.title_feedback
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
            selectedNavigationItem = selectedNavigationItem,
            onNavigationItemClick = {
                selectedNavigationItem = it
            },
            onDrawerCloseClick = { customDrawerState = CustomDrawerState.Closed }
        )
        Scaffold(
            modifier = modifier
                .offset{ IntOffset(x =  animatedOffset.roundToPx(), y = 0) }
                .scale(scale = animatedScale)
                .coloredShadow(
                    color = Color.Black,
                    alpha = 0.1f,
                    shadowRadius = 50.dp
                )
                .clickable(enabled = customDrawerState == CustomDrawerState.Opened) {
                    customDrawerState = CustomDrawerState.Closed
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
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            ) {
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

                if (isTopLevelDestination)
                    MmNavigationSuiteScaffold(
                        navigationSuiteItems = {
                            appState.topLevelDestination.forEach { destination ->
                                item(
                                    selected = currentDestination.isTopLevelDestinationInHierarchy(destination),
                                    onClick = { appState.navigateToTopLevelDestination(destination) },
                                    icon = { Icon(imageVector = destination.unSelectedIcon, contentDescription = null) },
                                    selectedIcon = { Icon(imageVector = destination.selectedIcon, contentDescription = null) },
                                    label = { Text(stringResource(destination.iconTextId)) },
                                )
                            }
                        },
                        windowAdaptiveInfo = windowAdaptiveInfo,
                    ) { GetContent(appState = appState) }
                else GetContent(appState = appState)
            }
        }
    }
}

@Composable
private fun GetContent(appState: MelodiQAppState) {
    Box(modifier = Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
        MelodiQNavHost(
            appState = appState,
            navigateToPlayer = { appState.navigateToPlayer() }
        )
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: TopLevelDestination) =
    this?.hierarchy?.any { it.route?.contains(destination.name, true) ?: false } ?: false