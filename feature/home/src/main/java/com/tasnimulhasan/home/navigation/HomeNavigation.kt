package com.tasnimulhasan.home.navigation

import com.tasnimulhasan.ui.motion.MelodiqMotion
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.tasnimulhasan.home.HomeScreen
import com.tasnimulhasan.home.navigation.HomeRoute
import kotlinx.serialization.Serializable

@Serializable object HomeRoute

fun NavController.navigateToHome(navOptions: NavOptions) = navigate(route = HomeRoute, navOptions)

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.homeScreen(
    navigateToPlayer: (musicId: String) -> Unit,
    navigateToSongDetails: (songId: Long) -> Unit,
    navigateToSearch: () -> Unit,
) {
    composable<HomeRoute>(
        enterTransition = { MelodiqMotion.fadeEnter() },
        exitTransition = { MelodiqMotion.fadeExit() },
        popEnterTransition = { MelodiqMotion.fadeEnter() },
        popExitTransition = { MelodiqMotion.fadeExit() }
    ) {
        SharedTransitionLayout {
            HomeScreen(
                navigateToPlayer = navigateToPlayer,
                navigateToSongDetails = navigateToSongDetails,
                navigateToSearch = navigateToSearch,
                animatedVisibilityScope = this@composable
            )
        }
    }
}