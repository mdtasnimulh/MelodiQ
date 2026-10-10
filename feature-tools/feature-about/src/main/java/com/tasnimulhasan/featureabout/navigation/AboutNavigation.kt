package com.tasnimulhasan.featureabout.navigation

import com.tasnimulhasan.ui.motion.MelodiqMotion
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
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        AboutRoute()
    }
}