package com.tasnimulhasan.melodiq.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.tasnimulhasan.designsystem.icon.MelodiqIcons
import com.tasnimulhasan.home.navigation.HomeRoute
import com.tasnimulhasan.library.navigation.LibraryHubNavRoute
import com.tasnimulhasan.playlists.navigation.PlaylistsRoute
import com.tasnimulhasan.songs.navigation.SongsRoute
import kotlin.reflect.KClass
import com.tasnimulhasan.designsystem.R as Res

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unSelectedIcon: ImageVector,
    @StringRes val iconTextId: Int,
    @StringRes val titleTextId: Int,
    val route: KClass<*>,
) {
    HOME(
        selectedIcon = MelodiqIcons.Home,
        unSelectedIcon = MelodiqIcons.HomeOutline,
        iconTextId = Res.string.title_home,
        titleTextId = Res.string.title_home,
        route = HomeRoute::class
    ),

    SONGS(
        selectedIcon = MelodiqIcons.Songs,
        unSelectedIcon = MelodiqIcons.SongsOutline,
        iconTextId = Res.string.title_songs,
        titleTextId = Res.string.title_songs,
        route = SongsRoute::class
    ),

    LIBRARY(
        selectedIcon = MelodiqIcons.Library,
        unSelectedIcon = MelodiqIcons.LibraryOutline,
        iconTextId = Res.string.title_library,
        titleTextId = Res.string.title_library,
        route = LibraryHubNavRoute::class
    ),

    PLAYLISTS(
        selectedIcon = MelodiqIcons.Playlists,
        unSelectedIcon = MelodiqIcons.PlaylistsOutline,
        iconTextId = Res.string.title_playlists,
        titleTextId = Res.string.title_playlists,
        route = PlaylistsRoute::class
    )
}