package com.tasnimulhasan.playlists.navigation

import com.tasnimulhasan.ui.motion.MelodiqMotion
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
    onPlaylistClicked: (Int, String) -> Unit,
) {
    composable<PlaylistsRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        PlaylistsRoute(
            onPlaylistClicked = { playlistId, playlistName -> onPlaylistClicked.invoke(playlistId, playlistName) }
        )
    }
}