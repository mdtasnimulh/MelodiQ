package com.tasnimulhasan.songs.navigation

import com.tasnimulhasan.ui.motion.MelodiqMotion
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.tasnimulhasan.songs.SongsRouteScreen
import kotlinx.serialization.Serializable

@Serializable object SongsRoute

fun NavController.navigateToSongs(navOptions: NavOptions) = navigate(route = SongsRoute, navOptions)

fun NavGraphBuilder.songsScreen(
    navigateToPlayer: (musicId: String) -> Unit,
    navigateToSearch: () -> Unit,
) {
    composable<SongsRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        SongsRouteScreen(navigateToPlayer = navigateToPlayer, navigateToSearch = navigateToSearch)
    }
}