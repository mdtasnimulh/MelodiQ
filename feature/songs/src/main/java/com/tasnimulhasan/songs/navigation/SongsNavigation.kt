package com.tasnimulhasan.songs.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.tasnimulhasan.songs.SongsRoute
import kotlinx.serialization.Serializable

@Serializable object SongsRoute

fun NavController.navigateToSongs(navOptions: NavOptions) = navigate(route = SongsRoute, navOptions)

fun NavGraphBuilder.songsScreen() {
    composable<SongsRoute>(
        enterTransition = { slideInHorizontally {it} },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        SongsRoute()
    }
}