package dev.maxxximgb.genesis.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.ui.components.EmptyState
import dev.maxxximgb.genesis.ui.components.LoadingState
import dev.maxxximgb.genesis.ui.components.TrackRow
import dev.maxxximgb.genesis.ui.library.components.AddToPlaylistSheet
import dev.maxxximgb.genesis.ui.library.components.SortMenu
import dev.maxxximgb.genesis.ui.nowPlaying.NowPlayingViewModel
import dev.maxxximgb.genesis.ui.settings.SettingsDialog
import dev.maxxximgb.genesis.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    addToPlaylistId: Long?,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    val pagingItems = libraryViewModel.pagedTracks.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()

    var showSettings by remember { mutableStateOf(false) }
    var pendingAddTrack by remember { mutableStateOf<Track?>(null) }
    var showAddSheetForSelection by remember { mutableStateOf(false) }

    val isAddRouteMode = addToPlaylistId != null
    val tracksAddedFmt = stringResource(R.string.snack_tracks_added)
    val trackAddedSingular = stringResource(R.string.snack_track_added)

    Scaffold(
        topBar = {
            if (isAddRouteMode) {
                TopAppBar(
                    title = { Text(stringResource(R.string.add_tracks)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                )
            } else if (uiState.selectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                R.string.selected_count,
                                uiState.selectedIds.size,
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = libraryViewModel::clearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddSheetForSelection = true }) {
                            Icon(Icons.Filled.PlaylistAdd, contentDescription = stringResource(R.string.add_to_playlist))
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.library_title)) },
                    actions = {
                        SortMenu(current = uiState.sort, onSortSelected = libraryViewModel::onSortChange)
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (isAddRouteMode) {
                AddTracksFooter(
                    selectedCount = uiState.selectedIds.size,
                    onConfirm = {
                        scope.launch {
                            val playlistId = addToPlaylistId ?: return@launch
                            val added = libraryViewModel.addSelectedToPlaylist(playlistId)
                            if (added > 0) {
                                snackbarHostState.showSnackbar(tracksAddedFmt.format(added))
                            }
                            onNavigateBack()
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                value = uiState.searchQuery,
                onValueChange = libraryViewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    pagingItems.loadState.refresh is LoadState.Loading -> LoadingState()
                    pagingItems.itemCount == 0 -> {
                        if (uiState.searchQuery.isNotEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.Search,
                                title = stringResource(R.string.empty_search_title),
                                subtitle = stringResource(R.string.empty_search_subtitle),
                            )
                        } else {
                            EmptyState(
                                icon = Icons.Filled.LibraryMusic,
                                title = stringResource(R.string.empty_library_title),
                                subtitle = stringResource(R.string.empty_library_subtitle),
                            )
                        }
                    }
                    else -> TrackList(
                        items = pagingItems,
                        selectedIds = uiState.selectedIds,
                        addRouteMode = isAddRouteMode,
                        onTrackClick = { track ->
                            when {
                                isAddRouteMode || uiState.selectionMode ->
                                    libraryViewModel.toggleSelection(track)
                                else -> nowPlayingViewModel.playSingle(track)
                            }
                        },
                        onTrackLongClick = libraryViewModel::toggleSelection,
                        onAddClick = { track -> pendingAddTrack = track },
                    )
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }

    val pendingTrack = pendingAddTrack
    if (pendingTrack != null) {
        AddToPlaylistSheet(
            playlists = playlists,
            onSelect = { playlist ->
                scope.launch {
                    libraryViewModel.addSingleToPlaylist(pendingTrack, playlist.id)
                    snackbarHostState.showSnackbar(trackAddedSingular)
                }
                pendingAddTrack = null
            },
            onDismiss = { pendingAddTrack = null },
        )
    }

    if (showAddSheetForSelection) {
        AddToPlaylistSheet(
            playlists = playlists,
            onSelect = { playlist ->
                scope.launch {
                    val added = libraryViewModel.addSelectedToPlaylist(playlist.id)
                    if (added > 0) {
                        snackbarHostState.showSnackbar(tracksAddedFmt.format(added))
                    }
                }
                showAddSheetForSelection = false
            },
            onDismiss = { showAddSheetForSelection = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                }
            }
        },
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
        modifier = modifier,
    )
}

@Composable
private fun TrackList(
    items: LazyPagingItems<Track>,
    selectedIds: Set<Long>,
    addRouteMode: Boolean,
    onTrackClick: (Track) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onAddClick: (Track) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = items.itemCount,
            key = items.itemKey { it.mediaStoreId },
        ) { index ->
            val track = items[index] ?: return@items
            val selected = track.mediaStoreId in selectedIds
            val selectionMode = addRouteMode || selectedIds.isNotEmpty()
            TrackRow(
                track = track,
                selected = selected,
                selectionMode = selectionMode,
                onClick = { onTrackClick(track) },
                onLongClick = { onTrackLongClick(track) },
                onAddClick = if (!selectionMode) {
                    { onAddClick(track) }
                } else null,
            )
        }
    }
}

@Composable
private fun AddTracksFooter(
    selectedCount: Int,
    onConfirm: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onConfirm,
            enabled = selectedCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(
                text = stringResource(R.string.add_n_tracks, selectedCount),
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
    }
}
