package com.tasnimulhasan.melodiq.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.tasnimulhasan.albums.navigation.albumDetailsScreen
import com.tasnimulhasan.albums.navigation.albumScreen
import com.tasnimulhasan.eqalizer.navigation.equalizerScreen
import com.tasnimulhasan.featureabout.navigation.aboutScreen
import com.tasnimulhasan.featurefavourite.navigation.favouriteScreen
import com.tasnimulhasan.featurefeedback.navigation.feedbackScreen
import com.tasnimulhasan.featureplayer.navigation.lyricsScreen
import com.tasnimulhasan.featureplayer.navigation.navigateToLyrics
import com.tasnimulhasan.featureplayer.navigation.playerScreen
import com.tasnimulhasan.featurequeue.navigation.queueScreen
import com.tasnimulhasan.home.navigation.HomeRoute
import com.tasnimulhasan.home.navigation.homeScreen
import com.tasnimulhasan.library.navigation.libraryScreens
import com.tasnimulhasan.melodiq.ui.MelodiQAppState
import com.tasnimulhasan.playlistdetails.navigation.playlistDetailsScreen
import com.tasnimulhasan.playlists.navigation.playlistsScreen
import com.tasnimulhasan.settings.navigation.settingsScreen
import com.tasnimulhasan.songdetails.navigation.navigateToSongDetails
import com.tasnimulhasan.songdetails.navigation.songDetailsScreen
import com.tasnimulhasan.songs.navigation.songsScreen

@Composable
fun MelodiQNavHost(
    appState: MelodiQAppState,
    modifier: Modifier = Modifier,
    navigateToPlayer: (String) -> Unit,
    navigateToEqualizerScreen: () -> Unit,
    onPlaylistClicked: (Int, String) -> Unit,
    navigateToAlbumDetails: (Long) -> Unit,
    navigateBack: () -> Unit,
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        homeScreen(
            navigateToPlayer = navigateToPlayer,
            navigateToSongDetails = { songId -> navController.navigateToSongDetails(songId) },
        )
        songsScreen(navigateToPlayer = navigateToPlayer)
        albumScreen(
            navigateToAlbumDetails = { albumId -> navigateToAlbumDetails(albumId) },
        )
        albumDetailsScreen(
            navigateBack = navigateBack,
            navigateToPlayer = navigateToPlayer,
        )
        playlistsScreen(
            onPlaylistClicked = { playlistId, playlistName ->
                onPlaylistClicked.invoke(playlistId, playlistName)
            }
        )
        settingsScreen()
        playerScreen(
            navigateBack = navigateBack,
            navigateToEqualizerScreen = navigateToEqualizerScreen,
            navigateToLyrics = { navController.navigateToLyrics() }
        )
        lyricsScreen(navigateBack = navigateBack)
        songDetailsScreen(navigateBack = navigateBack)
        queueScreen()
        favouriteScreen(navigateToPlayer = navigateToPlayer)
        libraryScreens(
            navController = navController,
            navigateToPlayer = navigateToPlayer,
            navigateBack = navigateBack,
        )
        aboutScreen()
        feedbackScreen()
        equalizerScreen()
        playlistDetailsScreen(
            navigateBack = navigateBack,
            navigateToPlayer = navigateToPlayer,
        )
    }
}