package com.tasnimulhasan.albums.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.tasnimulhasan.albums.AlbumsRoute
import kotlinx.serialization.Serializable

@Serializable object AlbumRoute

fun NavController.navigateToAlbums(navOptions: NavOptions) = navigate(route = AlbumRoute, navOptions)

fun NavGraphBuilder.albumScreen() {
    composable<AlbumRoute>(
        enterTransition = { slideInHorizontally {it} },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        AlbumsRoute()
    }
}