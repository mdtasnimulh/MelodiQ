package com.tasnimulhasan.featurefeedback.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.featurefeedback.FeedbackRoute
import kotlinx.serialization.Serializable

@Serializable object FeedbackRoute

fun NavController.navigateToFeedback(navOptions: NavOptionsBuilder.() -> Unit = {}){
    navigate(route = FeedbackRoute){
        navOptions()
    }
}

fun NavGraphBuilder.feedbackScreen() {
    composable<FeedbackRoute>(
        enterTransition = { slideInHorizontally {it} },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        FeedbackRoute()
    }
}