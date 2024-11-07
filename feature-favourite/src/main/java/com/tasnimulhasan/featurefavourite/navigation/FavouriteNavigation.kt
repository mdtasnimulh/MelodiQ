package com.tasnimulhasan.featurefavourite.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.tasnimulhasan.featurefavourite.FavouriteRoute
import kotlinx.serialization.Serializable

@Serializable object FavouriteRoute

fun NavController.navigateToFavourite(navOptions: NavOptionsBuilder.() -> Unit = {}){
    navigate(route = FavouriteRoute){
        navOptions()
    }
}

fun NavGraphBuilder.favouriteScreen() {
    composable<FavouriteRoute>(
        enterTransition = { slideInHorizontally {it} },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        FavouriteRoute()
    }
}