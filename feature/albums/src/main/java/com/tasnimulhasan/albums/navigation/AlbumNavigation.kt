package com.tasnimulhasan.albums.navigation

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
) {
    composable<AlbumRoute>(
        enterTransition = { slideInHorizontally {it} },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        AlbumsScreen(onAlbumClicked = navigateToAlbumDetails)
    }
}

fun NavGraphBuilder.albumDetailsScreen(
    navigateBack: () -> Unit,
    navigateToPlayer: (musicId: String) -> Unit,
) {
    composable<AlbumDetailsRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        AlbumDetailsScreen(navigateToPlayer = navigateToPlayer)
    }
}
