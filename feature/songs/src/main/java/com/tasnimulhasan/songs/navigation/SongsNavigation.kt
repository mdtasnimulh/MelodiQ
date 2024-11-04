package com.tasnimulhasan.songs.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.AnimationSpec
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
import com.tasnimulhasan.songs.SongsRoute
import com.tasnimulhasan.ui.DeepLinks.DEEP_LINK_SONGS
import com.tasnimulhasan.ui.NavRoutes.SONGS_ROUTE

fun NavController.navigateToSongs(navOptions: NavOptions) = navigate(SONGS_ROUTE, navOptions)

fun NavGraphBuilder.songsScreen() {
    composable(
        route = SONGS_ROUTE,
        deepLinks = listOf(
            navDeepLink { uriPattern = DEEP_LINK_SONGS },
        ),
        arguments = emptyList(),
    ) {
        SongsRoute()
    }
}