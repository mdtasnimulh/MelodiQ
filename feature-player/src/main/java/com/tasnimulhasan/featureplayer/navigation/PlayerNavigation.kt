package com.tasnimulhasan.featureplayer.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.tasnimulhasan.featureplayer.PlayerRoute
import com.tasnimulhasan.ui.DeepLinks.DEEP_LINK_PLAYER
import com.tasnimulhasan.ui.NavRoutes.PLAYER_ROUTE

fun NavController.navigateToPlayer(navOptions: NavOptions) = navigate(PLAYER_ROUTE, navOptions)

fun NavGraphBuilder.playerScreen() {
    composable(
        route = PLAYER_ROUTE,
        deepLinks = listOf(
            navDeepLink { uriPattern = DEEP_LINK_PLAYER },
        ),
        arguments = emptyList(),
    ) {
        PlayerRoute()
    }
}