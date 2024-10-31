package com.tasnimulhasan.featurefavourite.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.tasnimulhasan.featurefavourite.FavouriteRoute
import com.tasnimulhasan.ui.DeepLinks.DEEP_LINK_FAVOURITE
import com.tasnimulhasan.ui.NavRoutes.FAVOURITE_ROUTE

fun NavController.navigateToFavourite(navOptions: NavOptions) = navigate(FAVOURITE_ROUTE, navOptions)

fun NavGraphBuilder.favouriteScreen() {
    composable(
        route = FAVOURITE_ROUTE,
        deepLinks = listOf(
            navDeepLink { uriPattern = DEEP_LINK_FAVOURITE },
        ),
        arguments = emptyList(),
    ) {
        FavouriteRoute()
    }
}