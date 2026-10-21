package com.tasnimulhasan.songdetails.navigation

import com.tasnimulhasan.ui.motion.MelodiqMotion
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.songdetails.SongDetailsRoute
import kotlinx.serialization.Serializable

@Serializable data class SongDetailsNavRoute(val songId: Long)

fun NavController.navigateToSongDetails(songId: Long) = navigate(route = SongDetailsNavRoute(songId))

fun NavGraphBuilder.songDetailsScreen(
    navigateBack: () -> Unit,
) {
    composable<SongDetailsNavRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        SongDetailsRoute(
            onNavigateUp = navigateBack,
            onDeleted = navigateBack,
        )
    }
}
