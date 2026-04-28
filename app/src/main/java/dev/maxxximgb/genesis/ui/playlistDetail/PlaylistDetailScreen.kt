package dev.maxxximgb.genesis.ui.playlistDetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.ui.components.EmptyState
import dev.maxxximgb.genesis.ui.components.LoadingState
import dev.maxxximgb.genesis.ui.components.TrackRow
import dev.maxxximgb.genesis.ui.theme.Elevation
import dev.maxxximgb.genesis.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onAddTracks: (Long) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val tracksRemovedFmt = stringResource(R.string.snack_tracks_removed)

    Scaffold(
        topBar = {
            when (val s = state) {
                is PlaylistDetailUiState.Content -> {
                    if (s.selectionMode) {
                        TopAppBar(
                            title = {
                                Text(stringResource(R.string.selected_count, s.selectedIds.size))
                            },
                            navigationIcon = {
                                IconButton(onClick = viewModel::clearSelection) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                                }
                            },
                        )
                    } else {
                        TopAppBar(
                            title = { Text(s.detail.playlist.name) },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.play(0) },
                                    enabled = s.detail.tracks.isNotEmpty(),
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.play))
                                }
                            },
                        )
                    }
                }

                else -> TopAppBar(
                    title = { Text(stringResource(R.string.playlists_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            (state as? PlaylistDetailUiState.Content)?.let { content ->
                if (!content.selectionMode) {
                    ExtendedFloatingActionButton(
                        onClick = { onAddTracks(viewModel.playlistId) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.add_tracks)) },
                    )
                }
            }
        },
        bottomBar = {
            (state as? PlaylistDetailUiState.Content)?.takeIf { it.selectionMode }?.let { content ->
                SelectionActionBar(
                    onMoveUp = { viewModel.moveSelected(direction = -1) },
                    onMoveDown = { viewModel.moveSelected(direction = +1) },
                    onRemove = {
                        scope.launch {
                            val removed = viewModel.removeSelected()
                            if (removed > 0) {
                                snackbarHostState.showSnackbar(tracksRemovedFmt.format(removed))
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is PlaylistDetailUiState.Loading -> LoadingState()
                is PlaylistDetailUiState.NotFound -> EmptyState(
                    icon = Icons.Filled.PlaylistRemove,
                    title = stringResource(R.string.empty_playlist_title),
                    subtitle = null,
                )

                is PlaylistDetailUiState.Content -> {
                    val tracks = s.detail.tracks
                    if (tracks.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.PlaylistRemove,
                            title = stringResource(R.string.empty_playlist_title),
                            subtitle = stringResource(R.string.empty_playlist_subtitle),
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(
                                count = tracks.size,
                                key = { idx -> tracks[idx].mediaStoreId },
                            ) { idx ->
                                val track = tracks[idx]
                                val selected = track.mediaStoreId in s.selectedIds
                                TrackRow(
                                    track = track,
                                    selected = selected,
                                    selectionMode = s.selectionMode,
                                    onClick = {
                                        if (s.selectionMode) viewModel.toggleSelection(track.mediaStoreId)
                                        else viewModel.play(idx)
                                    },
                                    onLongClick = { viewModel.toggleSelection(track.mediaStoreId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionActionBar(
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(tonalElevation = Elevation.medium) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(onClick = onMoveUp) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = stringResource(R.string.move_up))
            }
            IconButton(onClick = onMoveDown) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = stringResource(R.string.move_down))
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = stringResource(R.string.remove_from_playlist),
                )
            }
        }
    }
}

