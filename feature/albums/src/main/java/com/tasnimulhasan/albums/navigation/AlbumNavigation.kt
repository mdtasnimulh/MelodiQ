package com.tasnimulhasan.albums.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.tasnimulhasan.albums.AlbumsRoute
import com.tasnimulhasan.ui.DeepLinks.DEEP_LINK_ALBUMS
import com.tasnimulhasan.ui.NavRoutes.ALBUMS_ROUTE

fun NavController.navigateToAlbums(navOptions: NavOptions) = navigate(ALBUMS_ROUTE, navOptions)

fun NavGraphBuilder.albumScreen() {
    composable(
        route = ALBUMS_ROUTE,
        deepLinks = listOf(
            navDeepLink { uriPattern = DEEP_LINK_ALBUMS },
        ),
        arguments = emptyList(),
    ) {
        AlbumsRoute()
    }
}