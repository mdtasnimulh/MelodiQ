package com.tasnimulhasan.eqalizer.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.eqalizer.EqualizerScreen
import kotlinx.serialization.Serializable

@Serializable object EqualizerRoute

fun NavController.navigateToEqualizer(navOptions: NavOptionsBuilder.() -> Unit = {}){
    navigate(route = EqualizerRoute){
        navOptions()
    }
}

fun NavGraphBuilder.equalizerScreen() {
    composable<EqualizerRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        EqualizerScreen()
    }
}