package com.tasnimulhasan.radio.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.radio.ui.RadioScreen
import com.tasnimulhasan.ui.motion.MelodiqMotion
import kotlinx.serialization.Serializable

@Serializable object RadioRoute

fun NavController.navigateToRadio(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = RadioRoute) {
        navOptions()
    }
}

fun NavGraphBuilder.radioScreen() {
    composable<RadioRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        RadioScreen()
    }
}
