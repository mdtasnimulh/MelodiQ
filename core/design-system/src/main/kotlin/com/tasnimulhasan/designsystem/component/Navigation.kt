package com.tasnimulhasan.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.designsystem.theme.MelodiqTheme

@Composable
fun RowScope.MelodiQNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    alwaysShowLabel: Boolean = true,
    icon: @Composable () -> Unit,
    selectedIcon: @Composable () -> Unit = icon,
    label: @Composable (() -> Unit)? = null
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = if (selected) selectedIcon else icon,
        modifier = modifier,
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MelodiQNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = MelodiQNavigationDefaults.navigationContentColor(),
            selectedTextColor = MelodiQNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = MelodiQNavigationDefaults.navigationContentColor(),
            indicatorColor = MelodiQNavigationDefaults.navigationIndicatorColor(),
        )
    )
}

@Composable
fun MelodiQNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    NavigationBar(
        modifier = modifier,
        contentColor = MelodiQNavigationDefaults.navigationContentColor(),
        tonalElevation = 0.dp,
        content = content,
    )
}

@Composable
fun MelodiQNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    alwaysShowLabel: Boolean = true,
    icon: @Composable () -> Unit,
    selectedIcon: @Composable () -> Unit = icon,
    label: @Composable (() -> Unit)? = null
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = if (selected) selectedIcon else icon,
        modifier = modifier,
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = MelodiQNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = MelodiQNavigationDefaults.navigationContentColor(),
            selectedTextColor = MelodiQNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = MelodiQNavigationDefaults.navigationContentColor(),
            indicatorColor = MelodiQNavigationDefaults.navigationIndicatorColor(),
        ),
    )
}

@Composable
fun MelodiQNavigationRail(
    modifier: Modifier = Modifier,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    NavigationRail(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MelodiQNavigationDefaults.navigationContentColor(),
        header = header,
        content = content,
    )
}

@Composable
fun MelodiQNavigationSuiteScaffold(
    navigationSuiteItems: MelodiQNavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
    content: @Composable () -> Unit,
) {
    val layoutType = NavigationSuiteScaffoldDefaults
        .calculateFromAdaptiveInfo(windowAdaptiveInfo)
    val navigationSuiteItemColors = NavigationSuiteItemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MelodiQNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = MelodiQNavigationDefaults.navigationContentColor(),
            selectedTextColor = MelodiQNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = MelodiQNavigationDefaults.navigationContentColor(),
            indicatorColor = MelodiQNavigationDefaults.navigationIndicatorColor(),
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = MelodiQNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = MelodiQNavigationDefaults.navigationContentColor(),
            selectedTextColor = MelodiQNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = MelodiQNavigationDefaults.navigationContentColor(),
            indicatorColor = MelodiQNavigationDefaults.navigationIndicatorColor(),
        ),
        navigationDrawerItemColors = NavigationDrawerItemDefaults.colors(
            selectedIconColor = MelodiQNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = MelodiQNavigationDefaults.navigationContentColor(),
            selectedTextColor = MelodiQNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = MelodiQNavigationDefaults.navigationContentColor(),
        ),
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            MelodiQNavigationSuiteScope(
                navigationSuiteScope = this,
                navigationSuiteItemColors = navigationSuiteItemColors,
            ).run(navigationSuiteItems)
        },
        layoutType = layoutType,
        containerColor = Color.Transparent,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContentColor = MelodiQNavigationDefaults.navigationContentColor(),
            navigationRailContainerColor = Color.Transparent,
        ),
        modifier = modifier,
    ) {
        content()
    }
}

class MelodiQNavigationSuiteScope internal constructor(
    private val navigationSuiteScope: NavigationSuiteScope,
    private val navigationSuiteItemColors: NavigationSuiteItemColors,
) {
    fun item(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: @Composable () -> Unit,
        selectedIcon: @Composable () -> Unit = icon,
        label: @Composable (() -> Unit)? = null,
    ) = navigationSuiteScope.item(
        selected = selected,
        onClick = onClick,
        icon = {
            if (selected) {
                selectedIcon()
            } else {
                icon()
            }
        },
        label = label,
        colors = navigationSuiteItemColors,
        modifier = modifier,
    )
}

@Preview
@Composable
fun MelodiQNavigationBarPreview() {
    val items = listOf("Home", "Songs", "Albums", "Playlists", "Settings")
    val icons = listOf(
        Icons.Filled.Home,
        Icons.Filled.MusicNote,
        Icons.Filled.Album,
        Icons.Filled.LibraryMusic,
        Icons.Filled.Settings
    )
    val selectedIcons = listOf(
        Icons.Outlined.Home,
        Icons.Outlined.MusicNote,
        Icons.Outlined.Album,
        Icons.Outlined.LibraryMusic,
        Icons.Outlined.Settings
    )

    MelodiqTheme {
        MelodiQNavigationBar {
            items.forEachIndexed { index, item ->
                MelodiQNavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = item,
                        )
                    },
                    selectedIcon = {
                        Icon(
                            imageVector = selectedIcons[index],
                            contentDescription = item,
                        )
                    },
                    label = { Text(item) },
                    selected = index == 0,
                    onClick = { },
                )
            }
        }
    }
}

@Preview
@Composable
fun MelodiQNavigationRailPreview() {
    val items = listOf("Home", "Songs", "Albums", "Playlists", "Settings")
    val icons = listOf(
        Icons.Filled.Home,
        Icons.Filled.MusicNote,
        Icons.Filled.Album,
        Icons.Filled.LibraryMusic,
        Icons.Filled.Settings
    )
    val selectedIcons = listOf(
        Icons.Outlined.Home,
        Icons.Outlined.MusicNote,
        Icons.Outlined.Album,
        Icons.Outlined.LibraryMusic,
        Icons.Outlined.Settings
    )

    MelodiqTheme {
        MelodiQNavigationRail {
            items.forEachIndexed { index, item ->
                MelodiQNavigationRailItem(
                    icon = {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = item,
                        )
                    },
                    selectedIcon = {
                        Icon(
                            imageVector = selectedIcons[index],
                            contentDescription = item,
                        )
                    },
                    label = { Text(item) },
                    selected = index == 0,
                    onClick = { },
                )
            }
        }
    }
}

object MelodiQNavigationDefaults {
    @Composable
    fun navigationContentColor() = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun navigationSelectedItemColor() = MaterialTheme.colorScheme.onPrimaryContainer

    @Composable
    fun navigationIndicatorColor() = MaterialTheme.colorScheme.primaryContainer
}