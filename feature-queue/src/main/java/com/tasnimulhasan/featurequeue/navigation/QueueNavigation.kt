package com.tasnimulhasan.featurequeue.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.tasnimulhasan.featurequeue.QueueRoute
import com.tasnimulhasan.ui.DeepLinks.DEEP_LINK_QUEUE
import com.tasnimulhasan.ui.NavRoutes.QUEUE_ROUTE

fun NavController.navigateToQueue(navOptions: NavOptions) = navigate(QUEUE_ROUTE, navOptions)

fun NavGraphBuilder.queueScreen() {
    composable(
        route = QUEUE_ROUTE,
        deepLinks = listOf(
            navDeepLink { uriPattern = DEEP_LINK_QUEUE },
        ),
        arguments = emptyList(),
    ) {
        QueueRoute()
    }
}