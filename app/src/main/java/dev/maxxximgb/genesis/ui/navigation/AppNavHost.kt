package dev.maxxximgb.genesis.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.maxxximgb.genesis.ui.library.LibraryScreen
import dev.maxxximgb.genesis.ui.playlistDetail.PlaylistDetailScreen
import dev.maxxximgb.genesis.ui.playlists.PlaylistsScreen

private const val NAV_ANIM_DURATION_MS = 200

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
            )
        }
        composable(Routes.Playlists.path) {
            PlaylistsScreen(
                snackbarHostState = snackbarHostState,
                onPlaylistClick = { navController.navigate(Routes.PlaylistDetail.forId(it)) },
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
    }
}
