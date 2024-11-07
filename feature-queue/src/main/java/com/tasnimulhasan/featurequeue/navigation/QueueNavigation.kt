package com.tasnimulhasan.featurequeue.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.featurequeue.QueueRoute
import kotlinx.serialization.Serializable

@Serializable object QueueRoute

fun NavController.navigateToQueue(navOptions: NavOptionsBuilder.() -> Unit = {}){
    navigate(route = QueueRoute){
        navOptions()
    }
}

fun NavGraphBuilder.queueScreen() {
    composable<QueueRoute>(
        enterTransition = { slideInHorizontally {it} },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        QueueRoute()
    }
}