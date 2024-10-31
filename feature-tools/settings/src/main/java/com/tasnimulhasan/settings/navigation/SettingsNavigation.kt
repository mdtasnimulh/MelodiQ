package com.tasnimulhasan.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.tasnimulhasan.settings.SettingsRoute
import com.tasnimulhasan.ui.DeepLinks.DEEP_LINK_SETTINGS
import com.tasnimulhasan.ui.NavRoutes.SETTINGS_ROUTE

fun NavController.navigateToSettings(navOptions: NavOptions) = navigate(SETTINGS_ROUTE, navOptions)

fun NavGraphBuilder.settingsScreen() {
    composable(
        route = SETTINGS_ROUTE,
        deepLinks = listOf(
            navDeepLink { uriPattern = DEEP_LINK_SETTINGS },
        ),
        arguments = emptyList(),
    ) {
        SettingsRoute()
    }
}
