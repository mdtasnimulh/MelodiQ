package com.tasnimulhasan.featureabout.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.tasnimulhasan.featureabout.AboutRoute
import com.tasnimulhasan.ui.DeepLinks.DEEP_LINK_ABOUT
import com.tasnimulhasan.ui.NavRoutes.ABOUT_ROUTE

fun NavController.navigateToAbout(navOptions: NavOptions) = navigate(ABOUT_ROUTE, navOptions)

fun NavGraphBuilder.aboutScreen() {
    composable(
        route = ABOUT_ROUTE,
        deepLinks = listOf(
            navDeepLink { uriPattern = DEEP_LINK_ABOUT },
        ),
        arguments = emptyList(),
    ) {
        AboutRoute()
    }
}