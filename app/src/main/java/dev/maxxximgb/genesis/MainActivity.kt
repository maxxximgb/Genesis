package dev.maxxximgb.genesis

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.maxxximgb.genesis.ui.components.BottomBar
import dev.maxxximgb.genesis.ui.components.NowPlayingBar
import dev.maxxximgb.genesis.ui.components.PermissionGate
import dev.maxxximgb.genesis.ui.navigation.AppNavHost
import dev.maxxximgb.genesis.ui.navigation.Routes
import dev.maxxximgb.genesis.ui.nowPlaying.NowPlayingViewModel
import dev.maxxximgb.genesis.ui.theme.GenesisTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingPlaylistId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntent(intent)
        setContent {
            GenesisTheme {
                PermissionGate {
                    AppRoot(
                        pendingPlaylistId = pendingPlaylistId,
                        onPendingPlaylistConsumed = { pendingPlaylistId = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_PLAYLIST_ID, -1L)?.takeIf { it > 0L }
        if (id != null) pendingPlaylistId = id
    }

    companion object {
        const val EXTRA_PLAYLIST_ID = "extra_playlist_id"
    }
}

@Composable
private fun AppRoot(
    pendingPlaylistId: Long?,
    onPendingPlaylistConsumed: () -> Unit,
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val playbackState by nowPlayingViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(pendingPlaylistId) {
        if (pendingPlaylistId != null) {
            navController.navigateToPlaylist(pendingPlaylistId)
            onPendingPlaylistConsumed()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = when {
        currentRoute == null -> true
        currentRoute.startsWith("playlist/") -> false
        currentRoute.startsWith("album/") -> false
        currentRoute.startsWith("artist/") -> false
        currentRoute.startsWith("folder/") -> false
        else -> true
    }
    val isPlaylistDetailHierarchy = backStackEntry?.destination?.hierarchy?.any {
        it.route == Routes.PlaylistDetail.pattern
    } == true

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                if (playbackState.title != null) {
                    NowPlayingBar(
                        state = playbackState,
                        onTogglePlayPause = nowPlayingViewModel::togglePlayPause,
                        onPrevious = nowPlayingViewModel::previous,
                        onNext = nowPlayingViewModel::next,
                    )
                }
                if (showBottomBar && !isPlaylistDetailHierarchy) {
                    BottomBar(navController = navController)
                }
            }
        },
    ) { padding ->
        AppNavHost(
            navController = navController,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

private fun NavHostController.navigateToPlaylist(playlistId: Long) {
    navigate(Routes.PlaylistDetail.forId(playlistId)) {
        launchSingleTop = true
    }
}
