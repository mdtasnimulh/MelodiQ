package com.tasnimulhasan.melodiq.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.tasnimulhasan.albums.navigation.albumScreen
import com.tasnimulhasan.featureabout.navigation.aboutScreen
import com.tasnimulhasan.featurefavourite.navigation.favouriteScreen
import com.tasnimulhasan.featurefeedback.navigation.feedbackScreen
import com.tasnimulhasan.featureplayer.navigation.playerScreen
import com.tasnimulhasan.featurequeue.navigation.queueScreen
import com.tasnimulhasan.home.navigation.homeScreen
import com.tasnimulhasan.melodiq.ui.MelodiqAppState
import com.tasnimulhasan.playlists.navigation.playlistsScreen
import com.tasnimulhasan.settings.navigation.settingsScreen
import com.tasnimulhasan.songs.navigation.songsScreen
import com.tasnimulhasan.ui.NavRoutes.HOME_ROUTE

@Composable
fun MelodiqNavHost(
    appState: MelodiqAppState,
    modifier: Modifier = Modifier,
    startDestination: String = HOME_ROUTE,
    navigateToPlayer: () -> Unit
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        homeScreen(navigateToPlayer = navigateToPlayer)
        songsScreen()
        albumScreen()
        playlistsScreen()
        settingsScreen()
        playerScreen()
        queueScreen()
        favouriteScreen()
        aboutScreen()
        feedbackScreen()
    }
}