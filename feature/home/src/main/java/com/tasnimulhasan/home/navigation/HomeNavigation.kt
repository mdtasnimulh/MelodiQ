package com.tasnimulhasan.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.tasnimulhasan.home.HomeRoute
import com.tasnimulhasan.ui.DeepLinks.DEEP_LINK_HOME
import com.tasnimulhasan.ui.NavRoutes.HOME_ROUTE

fun NavController.navigateToHome(navOptions: NavOptions) = navigate(HOME_ROUTE, navOptions)

fun NavGraphBuilder.homeScreen() {
    composable(
        route = HOME_ROUTE,
        deepLinks = listOf(
            navDeepLink { uriPattern = DEEP_LINK_HOME },
        ),
        arguments = emptyList(),
    ) {
        HomeRoute()
    }
}