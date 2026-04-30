package dev.maxxximgb.genesis.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import dev.maxxximgb.genesis.ui.components.TrackAction
import dev.maxxximgb.genesis.ui.components.TrackRow
import dev.maxxximgb.genesis.ui.library.components.AddToPlaylistSheet
import dev.maxxximgb.genesis.ui.library.components.LibrarySegmentControl
import dev.maxxximgb.genesis.ui.library.components.SearchHistoryDropdown
import dev.maxxximgb.genesis.ui.library.components.SortMenu
import dev.maxxximgb.genesis.ui.nowPlaying.NowPlayingViewModel
import dev.maxxximgb.genesis.ui.playlists.components.CreatePlaylistDialog
import dev.maxxximgb.genesis.ui.settings.SettingsDialog
import dev.maxxximgb.genesis.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    addToPlaylistId: Long?,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (Long) -> Unit = {},
    onNavigateToFolder: (Long) -> Unit = {},
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    val albums by libraryViewModel.albums.collectAsStateWithLifecycle()
    val artists by libraryViewModel.artists.collectAsStateWithLifecycle()
    val folders by libraryViewModel.folders.collectAsStateWithLifecycle()
    val pagingItems = libraryViewModel.pagedTracks.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()

    var showSettings by remember { mutableStateOf(false) }
    var pendingAddTrack by remember { mutableStateOf<Track?>(null) }
    var showAddSheetForSelection by remember { mutableStateOf(false) }
    var showCreatePlaylist by remember { mutableStateOf(false) }

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
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = stringResource(R.string.add_to_playlist))
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.library_title)) },
                    actions = {
                        if (uiState.browseTab == LibraryBrowseTab.ALBUMS &&
                            uiState.searchQuery.isEmpty()
                        ) {
                            IconButton(onClick = libraryViewModel::onAlbumsLayoutToggle) {
                                val isGrid = uiState.albumsLayout == AlbumsLayout.GRID
                                Icon(
                                    imageVector = if (isGrid) Icons.Filled.Menu else Icons.Filled.LibraryMusic,
                                    contentDescription = stringResource(R.string.toggle_layout),
                                )
                            }
                        }
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
        val density = LocalDensity.current
        var headerHeightPx by remember { mutableIntStateOf(0) }
        val headerOffsetPx = remember { mutableFloatStateOf(0f) }
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (headerHeightPx == 0) return Offset.Zero
                    val newOffset = (headerOffsetPx.floatValue + available.y)
                        .coerceIn(-headerHeightPx.toFloat(), 0f)
                    headerOffsetPx.floatValue = newOffset
                    return Offset.Zero
                }
            }
        }
        val headerHeightDp = with(density) { headerHeightPx.toDp() }
        val listContentPadding = androidx.compose.foundation.layout.PaddingValues(top = headerHeightDp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(nestedScrollConnection),
        ) {
            // Content (LazyColumn / LazyVerticalGrid receive scroll deltas)
            val showTracksList = isAddRouteMode || uiState.selectionMode ||
                uiState.searchQuery.isNotEmpty() ||
                uiState.browseTab == LibraryBrowseTab.TRACKS
            when {
                showTracksList -> TracksContent(
                    pagingItems = pagingItems,
                    uiState = uiState,
                    isAddRouteMode = isAddRouteMode,
                    onTrackClick = { track ->
                        when {
                            isAddRouteMode || uiState.selectionMode ->
                                libraryViewModel.toggleSelection(track)
                            else -> nowPlayingViewModel.playSingle(track)
                        }
                    },
                    onTrackLongClick = libraryViewModel::toggleSelection,
                    onAddClick = { track -> pendingAddTrack = track },
                    contentPadding = listContentPadding,
                )
                uiState.browseTab == LibraryBrowseTab.ALBUMS -> {
                    if (uiState.albumsLayout == AlbumsLayout.GRID) {
                        AlbumsGrid(
                            albums = albums,
                            onAlbumClick = { onNavigateToAlbum(it.id) },
                            contentPadding = listContentPadding,
                        )
                    } else {
                        AlbumsList(
                            albums = albums,
                            onAlbumClick = { onNavigateToAlbum(it.id) },
                            contentPadding = listContentPadding,
                        )
                    }
                }
                uiState.browseTab == LibraryBrowseTab.ARTISTS -> ArtistsList(
                    artists = artists,
                    onArtistClick = { onNavigateToArtist(it.id) },
                    contentPadding = listContentPadding,
                )
                uiState.browseTab == LibraryBrowseTab.FOLDERS -> FoldersList(
                    folders = folders,
                    onFolderClick = { onNavigateToFolder(it.bucketId) },
                    contentPadding = listContentPadding,
                )
            }

            // Header overlay (always full width, sliding via offset)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, headerOffsetPx.floatValue.toInt()) }
                    .onSizeChanged { headerHeightPx = it.height }
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                SearchField(
                    value = uiState.searchQuery,
                    onValueChange = libraryViewModel::onSearchQueryChange,
                    onSubmit = libraryViewModel::onSearchSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                )
                if (!isAddRouteMode && !uiState.selectionMode && uiState.searchQuery.isEmpty()) {
                    if (uiState.recentSearches.isNotEmpty()) {
                        SearchHistoryDropdown(
                            recent = uiState.recentSearches,
                            onClick = libraryViewModel::onRecentSearchClick,
                            onRemove = libraryViewModel::onRecentSearchRemove,
                        )
                    }
                    LibrarySegmentControl(
                        current = uiState.browseTab,
                        onSelect = libraryViewModel::onBrowseTabChange,
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
            onCreateNew = { showCreatePlaylist = true },
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
            onCreateNew = { showCreatePlaylist = true },
            onDismiss = { showAddSheetForSelection = false },
        )
    }

    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylist = false },
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    val pending = pendingAddTrack
                    val isSelectionFlow = showAddSheetForSelection
                    scope.launch {
                        val newId = libraryViewModel.createPlaylist(name)
                        when {
                            pending != null -> {
                                libraryViewModel.addSingleToPlaylist(pending, newId)
                                snackbarHostState.showSnackbar(trackAddedSingular)
                            }
                            isSelectionFlow -> {
                                val added = libraryViewModel.addSelectedToPlaylist(newId)
                                if (added > 0) {
                                    snackbarHostState.showSnackbar(tracksAddedFmt.format(added))
                                }
                            }
                        }
                    }
                }
                showCreatePlaylist = false
                pendingAddTrack = null
                showAddSheetForSelection = false
            },
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val textStyle = LocalTextStyle.current.merge(
        MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        )
    )

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Box(modifier = Modifier.weight(1f).padding(start = Spacing.sm)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(LocalContentColor.current),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { onSubmit() },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant,
                    )
                }
            }
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TracksContent(
    pagingItems: LazyPagingItems<Track>,
    uiState: LibraryUiState,
    isAddRouteMode: Boolean,
    onTrackClick: (Track) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onAddClick: (Track) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(0.dp),
) {
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
            currentMediaStoreId = uiState.currentMediaStoreId,
            contentPadding = contentPadding,
            onTrackClick = onTrackClick,
            onTrackLongClick = onTrackLongClick,
            onAddClick = onAddClick,
        )
    }
}

@Composable
private fun TrackList(
    items: LazyPagingItems<Track>,
    selectedIds: Set<Long>,
    addRouteMode: Boolean,
    currentMediaStoreId: Long?,
    onTrackClick: (Track) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onAddClick: (Track) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(0.dp),
) {
    val addLabel = stringResource(R.string.add_to_playlist)
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(
            count = items.itemCount,
            key = items.itemKey { it.mediaStoreId },
        ) { index ->
            val track = items[index] ?: return@items
            val selected = track.mediaStoreId in selectedIds
            val selectionMode = addRouteMode || selectedIds.isNotEmpty()
            val isCurrent = currentMediaStoreId != null && track.mediaStoreId == currentMediaStoreId
            TrackRow(
                track = track,
                selected = selected,
                selectionMode = selectionMode,
                isCurrentlyPlaying = isCurrent,
                onClick = { onTrackClick(track) },
                onLongClick = { onTrackLongClick(track) },
                actions = if (!selectionMode) {
                    listOf(
                        TrackAction(
                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                            label = addLabel,
                            onClick = { onAddClick(track) },
                        ),
                    )
                } else emptyList(),
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
