package com.tasnimulhasan.featurequeue.navigation

import com.tasnimulhasan.ui.motion.MelodiqMotion
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.featurequeue.QueueScreen
import kotlinx.serialization.Serializable

@Serializable object QueueRoute

fun NavController.navigateToQueue(navOptions: NavOptionsBuilder.() -> Unit = {}){
    navigate(route = QueueRoute){
        navOptions()
    }
}

fun NavGraphBuilder.queueScreen(navigateBack: () -> Unit = {}) {
    composable<QueueRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        QueueScreen(onNavigateUp = navigateBack)
    }
}