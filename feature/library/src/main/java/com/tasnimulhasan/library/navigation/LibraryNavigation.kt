package com.tasnimulhasan.library.navigation

import com.tasnimulhasan.ui.motion.MelodiqMotion
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.library.ArtistDetailsRoute
import com.tasnimulhasan.library.ArtistsRoute
import com.tasnimulhasan.library.DuplicatesRoute
import com.tasnimulhasan.library.FoldersRoute
import com.tasnimulhasan.library.GenreDetailsRoute
import com.tasnimulhasan.library.GenresRoute
import com.tasnimulhasan.library.LibraryHubRoute
import com.tasnimulhasan.library.NeverPlayedRoute
import com.tasnimulhasan.library.RecentlyPlayedRoute
import com.tasnimulhasan.library.SearchRoute
import com.tasnimulhasan.library.SmartPlaylistKind
import com.tasnimulhasan.library.SmartPlaylistRoute
import com.tasnimulhasan.library.SmartPlaylistsHubRoute
import com.tasnimulhasan.library.StatsRoute
import kotlinx.serialization.Serializable

@Serializable object LibraryHubNavRoute
@Serializable object ArtistsNavRoute
@Serializable data class ArtistDetailsNavRoute(val artist: String)
@Serializable object GenresNavRoute
@Serializable data class GenreDetailsNavRoute(val genre: String)
@Serializable object FoldersNavRoute
@Serializable object SearchNavRoute
@Serializable object RecentlyPlayedNavRoute
@Serializable object NeverPlayedNavRoute
@Serializable object SmartPlaylistsNavRoute
@Serializable data class SmartPlaylistNavRoute(val kind: String)
@Serializable object StatsNavRoute
@Serializable object DuplicatesNavRoute

fun NavController.navigateToLibraryHub(navOptions: androidx.navigation.NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = LibraryHubNavRoute) { navOptions() }
}
fun NavController.navigateToArtists() = navigate(route = ArtistsNavRoute)
fun NavController.navigateToArtistDetails(artist: String) = navigate(route = ArtistDetailsNavRoute(artist))
fun NavController.navigateToGenres() = navigate(route = GenresNavRoute)
fun NavController.navigateToGenreDetails(genre: String) = navigate(route = GenreDetailsNavRoute(genre))
fun NavController.navigateToFolders() = navigate(route = FoldersNavRoute)
fun NavController.navigateToSearch() = navigate(route = SearchNavRoute)
fun NavController.navigateToRecentlyPlayed() = navigate(route = RecentlyPlayedNavRoute)
fun NavController.navigateToNeverPlayed() = navigate(route = NeverPlayedNavRoute)
fun NavController.navigateToSmartPlaylists() = navigate(route = SmartPlaylistsNavRoute)
fun NavController.navigateToSmartPlaylist(kind: SmartPlaylistKind) = navigate(route = SmartPlaylistNavRoute(kind.name))
fun NavController.navigateToStats() = navigate(route = StatsNavRoute)
fun NavController.navigateToDuplicates() = navigate(route = DuplicatesNavRoute)

fun NavGraphBuilder.libraryScreens(
    navController: NavController,
    navigateToPlayer: (musicId: String) -> Unit,
    navigateBack: () -> Unit,
    navigateToAlbums: () -> Unit,
) {
    composable<LibraryHubNavRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        LibraryHubRoute(
            navigateToArtists = { navController.navigateToArtists() },
            navigateToGenres = { navController.navigateToGenres() },
            navigateToFolders = { navController.navigateToFolders() },
            navigateToRecentlyPlayed = { navController.navigateToRecentlyPlayed() },
            navigateToSearch = { navController.navigateToSearch() },
            navigateToNeverPlayed = { navController.navigateToNeverPlayed() },
            navigateToAlbums = navigateToAlbums,
            navigateToSmartPlaylists = { navController.navigateToSmartPlaylists() },
            navigateToStats = { navController.navigateToStats() },
            navigateToDuplicates = { navController.navigateToDuplicates() },
        )
    }

    composable<ArtistsNavRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        ArtistsRoute(onArtistClicked = { artist -> navController.navigateToArtistDetails(artist) })
    }

    composable<ArtistDetailsNavRoute>(
        enterTransition = { MelodiqMotion.fadeEnter() },
        exitTransition = { MelodiqMotion.fadeExit() },
        popEnterTransition = { MelodiqMotion.fadeEnter() },
        popExitTransition = { MelodiqMotion.fadeExit() }
    ) {
        ArtistDetailsRoute(navigateToPlayer = navigateToPlayer)
    }

    composable<GenresNavRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        GenresRoute(onGenreClicked = { genre -> navController.navigateToGenreDetails(genre) })
    }

    composable<GenreDetailsNavRoute>(
        enterTransition = { MelodiqMotion.fadeEnter() },
        exitTransition = { MelodiqMotion.fadeExit() },
        popEnterTransition = { MelodiqMotion.fadeEnter() },
        popExitTransition = { MelodiqMotion.fadeExit() }
    ) {
        GenreDetailsRoute(navigateToPlayer = navigateToPlayer)
    }

    composable<FoldersNavRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        FoldersRoute(navigateToPlayer = navigateToPlayer, navigateBack = navigateBack)
    }

    composable<SearchNavRoute>(
        enterTransition = { MelodiqMotion.fadeEnter() },
        exitTransition = { MelodiqMotion.fadeExit() },
        popEnterTransition = { MelodiqMotion.fadeEnter() },
        popExitTransition = { MelodiqMotion.fadeExit() }
    ) {
        SearchRoute(navigateToPlayer = navigateToPlayer)
    }

    composable<RecentlyPlayedNavRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        RecentlyPlayedRoute(navigateToPlayer = navigateToPlayer)
    }

    composable<NeverPlayedNavRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        NeverPlayedRoute(navigateToPlayer = navigateToPlayer)
    }

    composable<SmartPlaylistsNavRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        SmartPlaylistsHubRoute(
            navigateToSmartPlaylist = { kind -> navController.navigateToSmartPlaylist(kind) },
            navigateToNeverPlayed = { navController.navigateToNeverPlayed() },
        )
    }

    composable<SmartPlaylistNavRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        SmartPlaylistRoute(navigateToPlayer = navigateToPlayer)
    }

    composable<StatsNavRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        StatsRoute(navigateToPlayer = navigateToPlayer)
    }

    composable<DuplicatesNavRoute>(
        enterTransition = { MelodiqMotion.pushEnter() },
        exitTransition = { MelodiqMotion.pushExit() },
        popEnterTransition = { MelodiqMotion.popEnter() },
        popExitTransition = { MelodiqMotion.popExit() }
    ) {
        DuplicatesRoute()
    }
}
