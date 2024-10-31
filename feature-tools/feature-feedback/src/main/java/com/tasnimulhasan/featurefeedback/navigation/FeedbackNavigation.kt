package com.tasnimulhasan.featurefeedback.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.tasnimulhasan.featurefeedback.FeedbackRoute
import com.tasnimulhasan.ui.DeepLinks.DEEP_LINK_FEEDBACK
import com.tasnimulhasan.ui.NavRoutes.FEEDBACK_ROUTE

fun NavController.navigateToFeedback(navOptions: NavOptions) = navigate(FEEDBACK_ROUTE, navOptions)

fun NavGraphBuilder.feedbackScreen() {
    composable(
        route = FEEDBACK_ROUTE,
        deepLinks = listOf(
            navDeepLink { uriPattern = DEEP_LINK_FEEDBACK },
        ),
        arguments = emptyList(),
    ) {
        FeedbackRoute()
    }
}