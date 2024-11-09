package com.tasnimulhasan.melodiq.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.tasnimulhasan.albums.navigation.AlbumRoute
import com.tasnimulhasan.albums.navigation.navigateToAlbums
import com.tasnimulhasan.featureabout.navigation.navigateToAbout
import com.tasnimulhasan.featurefavourite.navigation.navigateToFavourite
import com.tasnimulhasan.featurefeedback.navigation.navigateToFeedback
import com.tasnimulhasan.featureplayer.navigation.navigateToPlayer
import com.tasnimulhasan.featurequeue.navigation.navigateToQueue
import com.tasnimulhasan.home.navigation.HomeRoute
import com.tasnimulhasan.home.navigation.navigateToHome
import com.tasnimulhasan.melodiq.navigation.TopLevelDestination
import com.tasnimulhasan.playlists.navigation.navigateToPlaylists
import com.tasnimulhasan.settings.navigation.navigateToSettings
import com.tasnimulhasan.songs.navigation.SongsRoute
import com.tasnimulhasan.songs.navigation.navigateToSongs
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberMelodiQAppState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
) : MelodiQAppState {
    return remember(
        navController,
        coroutineScope
    ) {
        MelodiQAppState(
            navController = navController,
            coroutineScope = coroutineScope
        )
    }
}

@Stable
class MelodiQAppState(
    val navController: NavHostController,
    coroutineScope: CoroutineScope
) {
    val currentDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination

    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() {
            return TopLevelDestination.entries.firstOrNull { topLevelDestination ->
                currentDestination?.hasRoute(route = topLevelDestination.route) ?: false
            }
        }

    val topLevelDestination: List<TopLevelDestination> = TopLevelDestination.entries

    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        val topLevelNavOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }

        when (topLevelDestination) {
            TopLevelDestination.HOME -> navController.navigateToHome(topLevelNavOptions)
            TopLevelDestination.SONGS -> navController.navigateToSongs(topLevelNavOptions)
            TopLevelDestination.ALBUMS -> navController.navigateToAlbums(topLevelNavOptions)
            TopLevelDestination.PLAYLISTS -> navController.navigateToPlaylists(topLevelNavOptions)
        }
    }

    fun navigateToPlayer(musicId: String) = navController.navigateToPlayer(musicId)

    fun navigateToQueue() = navController.navigateToQueue()

    fun navigateToFavourite() = navController.navigateToFavourite()

    fun navigateToAbout() = navController.navigateToAbout()

    fun navigateToFeedBack() = navController.navigateToFeedback()

    fun navigateToSettings() = navController.navigateToSettings()

    fun navigateBack() {
        navController.navigateUp()
    }
}