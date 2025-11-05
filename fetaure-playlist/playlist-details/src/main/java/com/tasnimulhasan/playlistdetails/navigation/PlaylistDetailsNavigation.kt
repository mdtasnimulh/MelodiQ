package com.tasnimulhasan.playlistdetails.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.playlistdetails.PlaylistDetailsScreen
import kotlinx.serialization.Serializable

@Serializable class PlaylistDetailsRoute(val playlistId: Int)

fun NavController.navigateToPlaylistDetails(playlistId: Int, navOptions: NavOptionsBuilder.() -> Unit = {}){
    navigate(route = PlaylistDetailsRoute(playlistId = playlistId)){
        navOptions()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.playlistDetailsScreen(
    navigateBack: () -> Unit,
) {
    composable<PlaylistDetailsRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) { backStackEntry ->
        val playlistId = backStackEntry.arguments?.getInt("playlistId") ?: -1
        SharedTransitionLayout {
            PlaylistDetailsScreen(
                playlistId = playlistId,
                onNavigateUp = navigateBack,
            )
        }
    }
}