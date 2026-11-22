package com.nigutu.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nigutu.app.feature.explore.ExploreScreen
import com.nigutu.app.feature.home.HomeScreen
import com.nigutu.app.feature.library.CollectionDetailScreen
import com.nigutu.app.feature.library.LibraryScreen
import com.nigutu.app.feature.library.PlaylistDetailScreen
import com.nigutu.app.feature.lyrics.LyricsScreen
import com.nigutu.app.feature.player.PlayerScreen
import com.nigutu.app.feature.player.QueueScreen
import com.nigutu.app.feature.search.SearchScreen
import com.nigutu.app.feature.settings.SettingsScreen

/** The complete navigation graph. */
@Composable
fun NigutuNavHost(
    navController: NavHostController,
    hasAudioAccess: Boolean,
    onRequestAudioAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        modifier = modifier,
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                hasAudioAccess = hasAudioAccess,
                onRequestAudioAccess = onRequestAudioAccess,
                onOpenPlayer = { navController.navigateToPlayer() },
                onAlbumClick = { album -> navController.navigate(Destination.Album.create(album)) },
                onArtistClick = { artist -> navController.navigate(Destination.Artist.create(artist)) },
                onOpenLibrary = { navController.navigate(Destination.Library.route) { launchSingleTop = true } },
            )
        }

        composable(Destination.Explore.route) {
            ExploreScreen(onOpenPlayer = { navController.navigateToPlayer() })
        }

        composable(Destination.Search.route) {
            SearchScreen(onOpenPlayer = { navController.navigateToPlayer() })
        }

        composable(Destination.Library.route) {
            LibraryScreen(
                onOpenPlayer = { navController.navigateToPlayer() },
                onPlaylistClick = { id -> navController.navigate(Destination.Playlist.create(id)) },
                onAlbumClick = { album -> navController.navigate(Destination.Album.create(album)) },
                onArtistClick = { artist -> navController.navigate(Destination.Artist.create(artist)) },
            )
        }

        composable(Destination.Settings.route) {
            SettingsScreen()
        }

        composable(Destination.Player.route) {
            PlayerScreen(
                onBack = { navController.popBackStack() },
                onOpenQueue = { navController.navigate(Destination.Queue.route) },
                onOpenLyrics = { navController.navigate(Destination.Lyrics.route) },
            )
        }

        composable(Destination.Queue.route) {
            QueueScreen(onBack = { navController.popBackStack() })
        }

        composable(Destination.Lyrics.route) {
            LyricsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Destination.Playlist.route,
            arguments = listOf(
                navArgument(Destination.Playlist.ARG_PLAYLIST_ID) { type = NavType.LongType },
            ),
        ) {
            PlaylistDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenPlayer = { navController.navigateToPlayer() },
            )
        }

        composable(
            route = Destination.Album.route,
            arguments = listOf(
                navArgument(Destination.Album.ARG_ALBUM_NAME) { type = NavType.StringType },
            ),
        ) {
            CollectionDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenPlayer = { navController.navigateToPlayer() },
            )
        }

        composable(
            route = Destination.Artist.route,
            arguments = listOf(
                navArgument(Destination.Artist.ARG_ARTIST_NAME) { type = NavType.StringType },
            ),
        ) {
            CollectionDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenPlayer = { navController.navigateToPlayer() },
            )
        }
    }
}

/** Opens the player without stacking duplicates. */
private fun NavHostController.navigateToPlayer() {
    navigate(Destination.Player.route) { launchSingleTop = true }
}
