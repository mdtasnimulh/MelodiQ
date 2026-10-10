package com.tasnimulhasan.playlistdetails.navigation

import com.tasnimulhasan.ui.motion.MelodiqMotion
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tasnimulhasan.playlistdetails.PlaylistDetailsScreen
import kotlinx.serialization.Serializable

@Serializable class PlaylistDetailsRoute(val playlistId: Int, val playlistName: String = "")

fun NavController.navigateToPlaylistDetails(
    playlistId: Int,
    playlistName: String = "",
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(route = PlaylistDetailsRoute(playlistId = playlistId, playlistName = playlistName)) {
        navOptions()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.playlistDetailsScreen(
    navigateBack: () -> Unit,
    navigateToPlayer: (musicId: String) -> Unit,
) {
    composable<PlaylistDetailsRoute>(
        enterTransition = { MelodiqMotion.fadeEnter() },
        exitTransition = { MelodiqMotion.fadeExit() },
        popEnterTransition = { MelodiqMotion.fadeEnter() },
        popExitTransition = { MelodiqMotion.fadeExit() }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<PlaylistDetailsRoute>()
        SharedTransitionLayout {
            PlaylistDetailsScreen(
                playlistId = route.playlistId,
                playlistName = route.playlistName,
                onNavigateUp = navigateBack,
                navigateToPlayer = navigateToPlayer,
            )
        }
    }
}