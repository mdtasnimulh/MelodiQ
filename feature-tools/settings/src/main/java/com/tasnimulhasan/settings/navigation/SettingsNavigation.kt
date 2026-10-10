package com.tasnimulhasan.settings.navigation

import com.tasnimulhasan.ui.motion.MelodiqMotion
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable object SettingsRoute

fun NavController.navigateToSettings(navOptions: NavOptionsBuilder.() -> Unit = {}){
    navigate(route = SettingsRoute){
        navOptions()
    }
}

fun NavGraphBuilder.settingsScreen() {
    composable<SettingsRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        SettingsRoute()
    }
}
