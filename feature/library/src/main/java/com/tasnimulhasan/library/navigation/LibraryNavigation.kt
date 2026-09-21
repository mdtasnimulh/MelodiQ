package com.tasnimulhasan.library.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tasnimulhasan.library.ArtistDetailsRoute
import com.tasnimulhasan.library.ArtistsRoute
import com.tasnimulhasan.library.FoldersRoute
import com.tasnimulhasan.library.GenreDetailsRoute
import com.tasnimulhasan.library.GenresRoute
import com.tasnimulhasan.library.LibraryHubRoute
import com.tasnimulhasan.library.RecentlyPlayedRoute
import com.tasnimulhasan.library.SearchRoute
import kotlinx.serialization.Serializable

@Serializable object LibraryHubNavRoute
@Serializable object ArtistsNavRoute
@Serializable data class ArtistDetailsNavRoute(val artist: String)
@Serializable object GenresNavRoute
@Serializable data class GenreDetailsNavRoute(val genre: String)
@Serializable object FoldersNavRoute
@Serializable object SearchNavRoute
@Serializable object RecentlyPlayedNavRoute

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

fun NavGraphBuilder.libraryScreens(
    navController: NavController,
    navigateToPlayer: (musicId: String) -> Unit,
    navigateBack: () -> Unit,
) {
    composable<LibraryHubNavRoute>(
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        LibraryHubRoute(
            navigateToArtists = { navController.navigateToArtists() },
            navigateToGenres = { navController.navigateToGenres() },
            navigateToFolders = { navController.navigateToFolders() },
            navigateToRecentlyPlayed = { navController.navigateToRecentlyPlayed() },
            navigateToSearch = { navController.navigateToSearch() },
        )
    }

    composable<ArtistsNavRoute>(
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        ArtistsRoute(onArtistClicked = { artist -> navController.navigateToArtistDetails(artist) })
    }

    composable<ArtistDetailsNavRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        ArtistDetailsRoute(navigateToPlayer = navigateToPlayer)
    }

    composable<GenresNavRoute>(
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        GenresRoute(onGenreClicked = { genre -> navController.navigateToGenreDetails(genre) })
    }

    composable<GenreDetailsNavRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        GenreDetailsRoute(navigateToPlayer = navigateToPlayer)
    }

    composable<FoldersNavRoute>(
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        FoldersRoute(navigateToPlayer = navigateToPlayer, navigateBack = navigateBack)
    }

    composable<SearchNavRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        SearchRoute(navigateToPlayer = navigateToPlayer)
    }

    composable<RecentlyPlayedNavRoute>(
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        RecentlyPlayedRoute(navigateToPlayer = navigateToPlayer)
    }
}
