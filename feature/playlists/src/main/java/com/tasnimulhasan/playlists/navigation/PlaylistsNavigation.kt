package com.tasnimulhasan.playlists.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.tasnimulhasan.playlists.PlaylistsRoute
import com.tasnimulhasan.ui.DeepLinks.DEEP_LINK_PLAYLISTS
import com.tasnimulhasan.ui.NavRoutes.PLAYLISTS_ROUTE

fun NavController.navigateToPlaylists(navOptions: NavOptions) = navigate(PLAYLISTS_ROUTE, navOptions)

fun NavGraphBuilder.playlistsScreen() {
    composable(
        route = PLAYLISTS_ROUTE,
        deepLinks = listOf(
            navDeepLink { uriPattern = DEEP_LINK_PLAYLISTS },
        ),
        arguments = emptyList(),
    ) {
        PlaylistsRoute()
    }
}