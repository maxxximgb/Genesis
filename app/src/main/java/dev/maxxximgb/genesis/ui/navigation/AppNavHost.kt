package dev.maxxximgb.genesis.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.maxxximgb.genesis.ui.equalizer.EqualizerScreen
import dev.maxxximgb.genesis.ui.library.AlbumDetailScreen
import dev.maxxximgb.genesis.ui.library.LibraryScreen
import dev.maxxximgb.genesis.ui.nowPlaying.FullNowPlayingScreen
import dev.maxxximgb.genesis.ui.playlistDetail.PlaylistDetailScreen
import dev.maxxximgb.genesis.ui.search.SearchScreen
import dev.maxxximgb.genesis.ui.settings.SettingsScreen

private const val NAV_ANIM_DURATION_MS = 200
private const val NOW_PLAYING_SLIDE_MS = 250

@Composable
fun AppNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Library.pattern,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION_MS)) },
        exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION_MS)) },
        popEnterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION_MS)) },
        popExitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION_MS)) },
    ) {
        composable(
            route = Routes.Library.pattern,
            arguments = listOf(
                navArgument(Routes.Library.ARG_ADD_TO_PLAYLIST_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val raw = entry.arguments?.getString(Routes.Library.ARG_ADD_TO_PLAYLIST_ID)
            val addToPlaylistId = raw?.toLongOrNull()
            LibraryScreen(
                addToPlaylistId = addToPlaylistId,
                snackbarHostState = snackbarHostState,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAlbum = { navController.navigate(Routes.AlbumDetail.forId(it)) },
                onNavigateToPlaylist = { navController.navigate(Routes.PlaylistDetail.forId(it)) },
                onNavigateToSearch = { navController.navigate(Routes.Search.path) },
                onNavigateToSettings = { navController.navigate(Routes.Settings.path) },
            )
        }
        composable(Routes.Search.path) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                snackbarHostState = snackbarHostState,
            )
        }
        composable(Routes.Settings.path) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEqualizer = { navController.navigate(Routes.Equalizer.path) },
            )
        }
        composable(Routes.Equalizer.path) {
            EqualizerScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.PlaylistDetail.pattern,
            arguments = listOf(
                navArgument(Routes.PlaylistDetail.ARG_PLAYLIST_ID) { type = NavType.LongType },
            ),
        ) {
            PlaylistDetailScreen(
                snackbarHostState = snackbarHostState,
                onNavigateBack = { navController.popBackStack() },
                onAddTracks = { playlistId ->
                    navController.navigate(Routes.Library.addToPlaylist(playlistId))
                },
            )
        }
        composable(
            route = Routes.AlbumDetail.pattern,
            arguments = listOf(
                navArgument(Routes.AlbumDetail.ARG_ALBUM_ID) { type = NavType.LongType },
            ),
        ) {
            AlbumDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                snackbarHostState = snackbarHostState,
            )
        }
        composable(
            route = Routes.NowPlaying.path,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(NOW_PLAYING_SLIDE_MS),
                )
            },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION_MS)) },
            popEnterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION_MS)) },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(NOW_PLAYING_SLIDE_MS),
                )
            },
        ) {
            FullNowPlayingScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEqualizer = { navController.navigate(Routes.Equalizer.path) },
            )
        }
    }
}
