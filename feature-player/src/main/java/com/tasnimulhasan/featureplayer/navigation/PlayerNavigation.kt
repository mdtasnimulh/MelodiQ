package com.tasnimulhasan.featureplayer.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.featureplayer.PlayerRoute
import kotlinx.serialization.Serializable

@Serializable object PlayerRoute

fun NavController.navigateToPlayer(navOptions: NavOptionsBuilder.() -> Unit = {}){
    navigate(route = PlayerRoute){
        navOptions()
    }
}

fun NavGraphBuilder.playerScreen() {
    composable<PlayerRoute>(
        enterTransition = { slideInHorizontally {it} },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        PlayerRoute()
    }
}