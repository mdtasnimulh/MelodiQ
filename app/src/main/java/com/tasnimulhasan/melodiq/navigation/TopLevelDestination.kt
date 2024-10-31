package com.tasnimulhasan.melodiq.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.tasnimulhasan.designsystem.icon.MelodiqIcons
import com.tasnimulhasan.designsystem.R as Res

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unSelectedIcon: ImageVector,
    val iconTextId: Int,
    val titleTextId: Int
) {
    HOME(
        selectedIcon = MelodiqIcons.Home,
        unSelectedIcon = MelodiqIcons.HomeOutline,
        iconTextId = Res.string.title_home,
        titleTextId = Res.string.title_home
    ),

    SONGS(
        selectedIcon = MelodiqIcons.Songs,
        unSelectedIcon = MelodiqIcons.SongsOutline,
        iconTextId = Res.string.title_songs,
        titleTextId = Res.string.title_songs
    ),

    ALBUMS(
        selectedIcon = MelodiqIcons.Album,
        unSelectedIcon = MelodiqIcons.AlbumOutline,
        iconTextId = Res.string.title_albums,
        titleTextId = Res.string.title_albums
    ),

    PLAYLISTS(
        selectedIcon = MelodiqIcons.Playlists,
        unSelectedIcon = MelodiqIcons.PlaylistsOutline,
        iconTextId = Res.string.title_playlists,
        titleTextId = Res.string.title_playlists
    ),

    SETTINGS(
        selectedIcon = MelodiqIcons.Settings,
        unSelectedIcon = MelodiqIcons.SettingsOutline,
        iconTextId = Res.string.title_settings,
        titleTextId = Res.string.title_settings
    )
}