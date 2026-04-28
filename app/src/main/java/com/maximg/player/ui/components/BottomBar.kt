package com.maximg.player.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.maximg.player.R

@Composable
fun BottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (currentRoute?.startsWith("playlist/") == true) return

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute?.startsWith("library") == true,
            onClick = {
                navController.navigate("library") {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = stringResource(R.string.library_title)
                )
            },
            label = { Text(stringResource(R.string.library_title)) }
        )
        NavigationBarItem(
            selected = currentRoute == "playlists",
            onClick = {
                navController.navigate("playlists") {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = stringResource(R.string.playlists_title)
                )
            },
            label = { Text(stringResource(R.string.playlists_title)) }
        )
    }
}
