package com.tasnimulhasan.melodiq.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.tasnimulhasan.albums.navigation.albumDetailsScreen
import com.tasnimulhasan.albums.navigation.albumScreen
import com.tasnimulhasan.eqalizer.navigation.equalizerScreen
import com.tasnimulhasan.featureabout.navigation.aboutScreen
import com.tasnimulhasan.featurefavourite.navigation.favouriteScreen
import com.tasnimulhasan.featurefeedback.navigation.feedbackScreen
import com.tasnimulhasan.featureaudiobook.navigation.audioBookScreens
import com.tasnimulhasan.featureaudiobook.navigation.navigateToBookDetails
import com.tasnimulhasan.featureaudiobook.navigation.navigateToBookReader
import com.tasnimulhasan.featureplayer.navigation.lyricsScreen
import com.tasnimulhasan.featureplayer.navigation.navigateToLyrics
import com.tasnimulhasan.featureplayer.navigation.playerScreen
import com.tasnimulhasan.featurequeue.navigation.navigateToQueue
import com.tasnimulhasan.featurequeue.navigation.queueScreen
import com.tasnimulhasan.home.navigation.HomeRoute
import com.tasnimulhasan.home.navigation.homeScreen
import com.tasnimulhasan.library.navigation.libraryScreens
import com.tasnimulhasan.library.navigation.navigateToSearch
import com.tasnimulhasan.melodiq.ui.MelodiQAppState
import com.tasnimulhasan.playlistdetails.navigation.playlistDetailsScreen
import com.tasnimulhasan.playlists.navigation.playlistsScreen
import com.tasnimulhasan.settings.navigation.settingsScreen
import com.tasnimulhasan.songdetails.navigation.navigateToSongDetails
import com.tasnimulhasan.songdetails.navigation.songDetailsScreen
import com.tasnimulhasan.songs.navigation.songsScreen
import com.tasnimulhasan.designsystem.theme.isGlassUi

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
    // Glass screens are transparent, so the default cross-slide would show two screens through
    // each other. In Glass mode use a short fade-through instead; the solid UI keeps the defaults.
    val glass = isGlassUi()
    val glassOut = fadeOut(tween(90))
    val glassIn = fadeIn(tween(210, delayMillis = 90))
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = if (glass) ({ glassIn }) else ({ androidx.compose.animation.fadeIn(tween(700)) }),
        exitTransition = if (glass) ({ glassOut }) else ({ androidx.compose.animation.fadeOut(tween(700)) }),
        popEnterTransition = if (glass) ({ glassIn }) else ({ androidx.compose.animation.fadeIn(tween(700)) }),
        popExitTransition = if (glass) ({ glassOut }) else ({ androidx.compose.animation.fadeOut(tween(700)) }),
    ) {
        homeScreen(
            navigateToPlayer = navigateToPlayer,
            navigateToSongDetails = { songId -> navController.navigateToSongDetails(songId) },
            navigateToSearch = { navController.navigateToSearch() },
        )
        songsScreen(
            navigateToPlayer = navigateToPlayer,
            navigateToSearch = { navController.navigateToSearch() },
            navigateToSongDetails = { songId -> navController.navigateToSongDetails(songId) },
        )
        albumScreen(
            navigateToAlbumDetails = { albumId -> navigateToAlbumDetails(albumId) },
            navigateToSearch = { navController.navigateToSearch() },
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
            navigateToLyrics = { navController.navigateToLyrics() },
            navigateToQueue = { navController.navigateToQueue() }
        )
        lyricsScreen(navigateBack = navigateBack)
        songDetailsScreen(navigateBack = navigateBack)
        queueScreen(navigateBack = navigateBack)
        favouriteScreen(navigateToPlayer = navigateToPlayer)
        libraryScreens(
            navController = navController,
            navigateToPlayer = navigateToPlayer,
            navigateBack = navigateBack,
            navigateToAlbums = { appState.navigateToAlbumsList() },
        )
        aboutScreen()
        audioBookScreens(
            navigateToDetails = { bookId -> navController.navigateToBookDetails(bookId) },
            navigateToReader = { bookId, autoPlay -> navController.navigateToBookReader(bookId, autoPlay) },
            navigateBack = navigateBack,
        )
        feedbackScreen()
        equalizerScreen()
        playlistDetailsScreen(
            navigateBack = navigateBack,
            navigateToPlayer = navigateToPlayer,
        )
    }
}