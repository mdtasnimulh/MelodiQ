package com.tasnimulhasan.playlists.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.tasnimulhasan.playlists.PlaylistsRoute
import kotlinx.serialization.Serializable

@Serializable
object PlaylistsRoute

fun NavController.navigateToPlaylists(navOptions: NavOptions) =
    navigate(route = PlaylistsRoute, navOptions)

fun NavGraphBuilder.playlistsScreen(
    onPlaylistClicked: (Int) -> Unit,
) {
    composable<PlaylistsRoute>(
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        PlaylistsRoute(
            onPlaylistClicked = { playlistId -> onPlaylistClicked.invoke(playlistId) }
        )
    }
}