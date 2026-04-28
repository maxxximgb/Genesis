package dev.maxxximgb.genesis

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GenesisTheme {
                PermissionGate {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot(
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val playbackState by nowPlayingViewModel.uiState.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = when {
        currentRoute == null -> true
        currentRoute.startsWith("playlist/") -> false
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
