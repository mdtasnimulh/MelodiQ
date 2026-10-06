package com.tasnimulhasan.featureplayer.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.featureplayer.PlayerScreen
import com.tasnimulhasan.featureplayer.lyrics.LyricsRoute
import kotlinx.serialization.Serializable

@Serializable class PlayerRoute(val musicId: String)
@Serializable object LyricsNavRoute

fun NavController.navigateToPlayer(musicId: String, navOptions: NavOptionsBuilder.() -> Unit = {}){
    navigate(route = PlayerRoute(musicId = musicId)){
        navOptions()
    }
}

fun NavController.navigateToLyrics() = navigate(route = LyricsNavRoute)

fun NavGraphBuilder.lyricsScreen(navigateBack: () -> Unit) {
    composable<LyricsNavRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        LyricsRoute(onNavigateUp = navigateBack)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.playerScreen(
    navigateBack: () -> Unit,
    navigateToEqualizerScreen: () -> Unit,
    navigateToLyrics: () -> Unit,
    navigateToQueue: () -> Unit,
) {
    composable<PlayerRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) { backStackEntry ->
        val musicId = backStackEntry.arguments?.getString("musicId") ?: ""
        SharedTransitionLayout {
            PlayerScreen(
                musicId = musicId,
                onNavigateUp = navigateBack,
                navigateToEqualizerScreen = navigateToEqualizerScreen,
                navigateToLyrics = navigateToLyrics,
                navigateToQueue = navigateToQueue,
                animatedVisibilityScope = this@composable
            )
        }
    }
}