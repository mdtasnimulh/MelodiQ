package com.tasnimulhasan.home.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.tasnimulhasan.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable object HomeRoute

fun NavController.navigateToHome(navOptions: NavOptions) = navigate(route = HomeRoute, navOptions)

fun NavGraphBuilder.homeScreen(
    navigateToPlayer: () -> Unit
) {
    composable<HomeRoute>(
        enterTransition = { slideInHorizontally {it} },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        HomeRoute(navigateToPlayer = navigateToPlayer)
    }
}