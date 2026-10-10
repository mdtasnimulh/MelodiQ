package com.tasnimulhasan.albums.navigation

import com.tasnimulhasan.ui.motion.MelodiqMotion
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.albums.AlbumDetailsScreen
import com.tasnimulhasan.albums.AlbumsScreen
import kotlinx.serialization.Serializable

@Serializable object AlbumRoute

@Serializable data class AlbumDetailsRoute(val albumId: Long)

fun NavController.navigateToAlbums(navOptions: NavOptions) = navigate(route = AlbumRoute, navOptions)

fun NavController.navigateToAlbumDetails(albumId: Long, navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = AlbumDetailsRoute(albumId = albumId)) {
        navOptions()
    }
}

fun NavGraphBuilder.albumScreen(
    navigateToAlbumDetails: (albumId: Long) -> Unit,
    navigateToSearch: () -> Unit,
) {
    composable<AlbumRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        AlbumsScreen(onAlbumClicked = navigateToAlbumDetails, navigateToSearch = navigateToSearch)
    }
}

fun NavGraphBuilder.albumDetailsScreen(
    navigateBack: () -> Unit,
    navigateToPlayer: (musicId: String) -> Unit,
) {
    composable<AlbumDetailsRoute>(
        enterTransition = { MelodiqMotion.fadeEnter() },
        exitTransition = { MelodiqMotion.fadeExit() },
        popEnterTransition = { MelodiqMotion.fadeEnter() },
        popExitTransition = { MelodiqMotion.fadeExit() }
    ) {
        AlbumDetailsScreen(navigateToPlayer = navigateToPlayer)
    }
}
