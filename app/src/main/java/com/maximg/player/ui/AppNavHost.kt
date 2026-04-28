package com.maximg.player.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.maximg.player.ui.components.BottomBar
import com.maximg.player.ui.components.NowPlayingBar
import com.maximg.player.ui.screens.LibraryScreen
import com.maximg.player.ui.screens.PlaylistDetailScreen
import com.maximg.player.ui.screens.PlaylistsScreen

@Composable
fun AppNavHost(startPlaylistId: Long?) {
    val navController = rememberNavController()

    LaunchedEffect(startPlaylistId) {
        if (startPlaylistId != null) {
            navController.navigate("playlist/$startPlaylistId")
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                NowPlayingBar()
                BottomBar(navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "library?addToPlaylistId={addToPlaylistId}",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(
                route = "library?addToPlaylistId={addToPlaylistId}",
                arguments = listOf(
                    navArgument("addToPlaylistId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val addToPlaylistId = backStackEntry.arguments
                    ?.getString("addToPlaylistId")
                    ?.toLongOrNull()
                LibraryScreen(
                    addToPlaylistId = addToPlaylistId,
                    onAddedToPlaylist = { navController.popBackStack() }
                )
            }
            composable("playlists") {
                PlaylistsScreen(onOpenPlaylist = { id -> navController.navigate("playlist/$id") })
            }
            composable(
                route = "playlist/{playlistId}",
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    onAddTracks = { id ->
                        navController.navigate("library?addToPlaylistId=$id")
                    }
                )
            }
        }
    }
}
