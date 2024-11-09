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
import com.tasnimulhasan.home.navigation.HomeRoute
import com.tasnimulhasan.home.navigation.homeScreen
import com.tasnimulhasan.melodiq.ui.MelodiQAppState
import com.tasnimulhasan.playlists.navigation.playlistsScreen
import com.tasnimulhasan.settings.navigation.settingsScreen
import com.tasnimulhasan.songs.navigation.songsScreen

@Composable
fun MelodiQNavHost(
    appState: MelodiQAppState,
    modifier: Modifier = Modifier,
    navigateToPlayer: (String) -> Unit
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
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