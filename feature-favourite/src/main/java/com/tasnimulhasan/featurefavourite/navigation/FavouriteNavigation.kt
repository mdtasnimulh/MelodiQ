package com.tasnimulhasan.featurefavourite.navigation

import com.tasnimulhasan.ui.motion.MelodiqMotion
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.tasnimulhasan.featurefavourite.FavouriteRouteScreen
import kotlinx.serialization.Serializable

@Serializable object FavouriteRoute

fun NavController.navigateToFavourite(navOptions: NavOptionsBuilder.() -> Unit = {}){
    navigate(route = FavouriteRoute){
        navOptions()
    }
}

fun NavGraphBuilder.favouriteScreen(
    navigateToPlayer: (musicId: String) -> Unit,
) {
    composable<FavouriteRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        FavouriteRouteScreen(navigateToPlayer = navigateToPlayer)
    }
}