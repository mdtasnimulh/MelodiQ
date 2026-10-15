package com.tasnimulhasan.featureaudiobook.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tasnimulhasan.featureaudiobook.AudioBookRouteScreen
import com.tasnimulhasan.featureaudiobook.BookDetailsRouteScreen
import com.tasnimulhasan.ui.motion.MelodiqMotion
import kotlinx.serialization.Serializable

@Serializable object AudioBookRoute

@Serializable class AudioBookDetailsRoute(val bookId: String)

fun NavController.navigateToAudioBook(navOptions: NavOptions? = null) = navigate(route = AudioBookRoute, navOptions)

fun NavController.navigateToBookDetails(bookId: String) = navigate(route = AudioBookDetailsRoute(bookId))

fun NavGraphBuilder.audioBookScreens(
    navigateToDetails: (bookId: String) -> Unit,
    navigateBack: () -> Unit,
) {
    composable<AudioBookRoute> {
        AudioBookRouteScreen(navigateToDetails = navigateToDetails)
    }
    composable<AudioBookDetailsRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() },
    ) { entry ->
        val route = entry.toRoute<AudioBookDetailsRoute>()
        BookDetailsRouteScreen(bookId = route.bookId, navigateBack = navigateBack)
    }
}
