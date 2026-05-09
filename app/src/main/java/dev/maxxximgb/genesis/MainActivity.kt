package dev.maxxximgb.genesis

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
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
    val currentAlbumId by nowPlayingViewModel.currentAlbumId.collectAsStateWithLifecycle()
    val isSelectionMode by nowPlayingViewModel.isSelectionMode.collectAsStateWithLifecycle()
    val isMiniBarDismissed by nowPlayingViewModel.isMiniBarDismissed.collectAsStateWithLifecycle()

    LaunchedEffect(pendingPlaylistId) {
        if (pendingPlaylistId != null) {
            navController.navigateToPlaylist(pendingPlaylistId)
            onPendingPlaylistConsumed()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isNowPlayingRoute = currentRoute == Routes.NowPlaying.path

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Slide the mini bar down out of the way while the user is multi-selecting
            // tracks in the library; reappear when selection is cleared.
            AnimatedVisibility(
                visible = playbackState.title != null && !isNowPlayingRoute && !isSelectionMode && !isMiniBarDismissed,
                enter = slideInVertically(tween(BAR_ANIM_MS)) { it } + fadeIn(tween(BAR_ANIM_MS)),
                exit = slideOutVertically(tween(BAR_ANIM_MS)) { it } + fadeOut(tween(BAR_ANIM_MS)),
            ) {
                NowPlayingBar(
                    state = playbackState,
                    albumId = currentAlbumId,
                    onTogglePlayPause = nowPlayingViewModel::togglePlayPause,
                    onPrevious = nowPlayingViewModel::previous,
                    onNext = nowPlayingViewModel::next,
                    onTap = {
                        nowPlayingViewModel.clearMiniBarDismiss()
                        // popUpTo + launchSingleTop guarantee a single NowPlaying entry on the
                        // back stack: tapping the mini bar from a sub-screen of NowPlaying
                        // (e.g. Equalizer) pops everything above the existing NowPlaying entry
                        // instead of stacking another one. Back from NowPlaying always returns
                        // straight to its parent (Library), no double-press needed.
                        navController.navigate(Routes.NowPlaying.path) {
                            popUpTo(Routes.NowPlaying.path) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onDismiss = nowPlayingViewModel::dismissMiniBar,
                )
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

private const val BAR_ANIM_MS = 220
