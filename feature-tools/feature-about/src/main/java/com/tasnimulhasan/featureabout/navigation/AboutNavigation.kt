package com.tasnimulhasan.featureabout.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.featureabout.AboutRoute
import kotlinx.serialization.Serializable

@Serializable object AboutRoute

fun NavController.navigateToAbout(navOptions: NavOptionsBuilder.() -> Unit = {}){
    navigate(route = AboutRoute){
        navOptions()
    }
}

fun NavGraphBuilder.aboutScreen() {
    composable<AboutRoute>(
        enterTransition = { slideInHorizontally {it} },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        AboutRoute()
    }
}