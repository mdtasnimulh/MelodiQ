package com.tasnimulhasan.featureplayer.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.featureplayer.PlayerScreen
import kotlinx.serialization.Serializable

@Serializable class PlayerRoute(val musicId: String)

fun NavController.navigateToPlayer(musicId: String, navOptions: NavOptionsBuilder.() -> Unit = {}){
    navigate(route = PlayerRoute(musicId = musicId)){
        navOptions()
    }
}

fun NavGraphBuilder.playerScreen(
    navigateBack: () -> Unit,
) {
    composable<PlayerRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) { backStackEntry ->
        val musicId = backStackEntry.arguments?.getString("musicId") ?: ""
        PlayerScreen(musicId = musicId, onNavigateUp = navigateBack)
    }
}