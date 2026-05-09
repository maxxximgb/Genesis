package dev.maxxximgb.genesis.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.verticalDrag
import kotlinx.coroutines.isActive
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
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
import dev.maxxximgb.genesis.ui.theme.Sizes
import dev.maxxximgb.genesis.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SELECTION_ANIM_MS = 220

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    addToPlaylistId: Long?,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToPlaylist: (Long) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    val playlistSummaries by libraryViewModel.playlistSummaries.collectAsStateWithLifecycle()
    val albums by libraryViewModel.albums.collectAsStateWithLifecycle()
    val audiobookOverrides by libraryViewModel.audiobookOverrides.collectAsStateWithLifecycle()
    val pagingItems = libraryViewModel.pagedTracks.collectAsLazyPagingItems()
    val audiobookItems = libraryViewModel.pagedAudiobooks.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    // Activity-level scope for fire-and-forget UI work that must outlive this screen
    // (e.g. snackbars shown immediately after popBackStack — `scope` would be cancelled
    // when LibraryScreen disposes during the nav, swallowing the snackbar before it
    // could appear).
    val appScope = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycleScope

    var pendingAddTrack by remember { mutableStateOf<Track?>(null) }
    var showAddSheetForSelection by remember { mutableStateOf(false) }
    var showAddSheetForAlbumSelection by remember { mutableStateOf(false) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var pendingRenamePlaylist by remember {
        mutableStateOf<dev.maxxximgb.genesis.domain.model.Playlist?>(null)
    }
    var pendingDeletePlaylist by remember {
        mutableStateOf<dev.maxxximgb.genesis.domain.model.Playlist?>(null)
    }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    val playlistRenamedMsg = stringResource(R.string.snack_playlist_renamed)
    val playlistDeletedMsg = stringResource(R.string.snack_playlist_deleted)

    val isAddRouteMode = addToPlaylistId != null
    val tracksAddedFmt = stringResource(R.string.snack_tracks_added)
    val trackAddedSingular = stringResource(R.string.snack_track_added)
    val markedAudiobookFmt = stringResource(R.string.snack_marked_audiobooks)
    val unmarkedAudiobookFmt = stringResource(R.string.snack_unmarked_audiobooks)
    val addedToQueueFmt = stringResource(R.string.snack_added_to_queue)

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
                        // On the Audiobooks tab the bulk action flips: instead of marking
                        // tracks as audiobooks, we unmark them (only override-marked tracks
                        // are removable; native IS_AUDIOBOOK rows stay).
                        if (uiState.browseTab == LibraryBrowseTab.AUDIOBOOKS) {
                            IconButton(onClick = {
                                scope.launch {
                                    val n = libraryViewModel.unmarkSelectedAudiobooks()
                                    if (n > 0) {
                                        snackbarHostState.showSnackbar(
                                            unmarkedAudiobookFmt.format(n),
                                        )
                                    }
                                }
                            }) {
                                Icon(
                                    Icons.Filled.BookmarkRemove,
                                    contentDescription = stringResource(R.string.unmark_audiobook),
                                )
                            }
                        } else {
                            IconButton(onClick = {
                                scope.launch {
                                    val n = libraryViewModel.markSelectedAsAudiobooks()
                                    if (n > 0) {
                                        snackbarHostState.showSnackbar(
                                            markedAudiobookFmt.format(n),
                                        )
                                    }
                                }
                            }) {
                                Icon(
                                    Icons.Filled.BookmarkAdd,
                                    contentDescription = stringResource(R.string.mark_as_audiobook),
                                )
                            }
                        }
                    },
                )
            } else if (uiState.playlistSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                R.string.selected_count,
                                uiState.selectedPlaylistIds.size,
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = libraryViewModel::clearPlaylistSelection) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                val n = libraryViewModel.markSelectedPlaylistsAsAudiobook()
                                if (n > 0) {
                                    snackbarHostState.showSnackbar(markedAudiobookFmt.format(n))
                                }
                            }
                        }) {
                            Icon(
                                Icons.Filled.MenuBook,
                                contentDescription = stringResource(R.string.mark_as_audiobook),
                            )
                        }
                        IconButton(onClick = { showBulkDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    },
                )
            } else if (uiState.albumSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                R.string.selected_count,
                                uiState.selectedAlbumIds.size,
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = libraryViewModel::clearAlbumSelection) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddSheetForAlbumSelection = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = stringResource(R.string.add_to_playlist),
                            )
                        }
                        IconButton(onClick = {
                            scope.launch {
                                val n = libraryViewModel.markSelectedAlbumsAsAudiobook()
                                if (n > 0) {
                                    snackbarHostState.showSnackbar(markedAudiobookFmt.format(n))
                                }
                            }
                        }) {
                            Icon(
                                Icons.Filled.MenuBook,
                                contentDescription = stringResource(R.string.mark_as_audiobook),
                            )
                        }
                    },
                )
            } else {
                // 36dp icon buttons — M3's default 48dp creates ~10dp of vertical slop
                // around the title text. SortMenu is capped to 36dp internally so it
                // doesn't drag the row taller than its siblings.
                val iconButtonSize = Modifier.size(36.dp)
                val iconGlyphSize = Modifier.size(20.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(start = Spacing.md, end = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.library_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onNavigateToSearch, modifier = iconButtonSize) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search_placeholder),
                            modifier = iconGlyphSize,
                        )
                    }
                    if (uiState.browseTab == LibraryBrowseTab.ALBUMS) {
                        IconButton(
                            onClick = libraryViewModel::onAlbumsLayoutToggle,
                            modifier = iconButtonSize,
                        ) {
                            val isGrid = uiState.albumsLayout == AlbumsLayout.GRID
                            Icon(
                                imageVector = if (isGrid) Icons.Filled.Menu else Icons.Filled.LibraryMusic,
                                contentDescription = stringResource(R.string.toggle_layout),
                                modifier = iconGlyphSize,
                            )
                        }
                    }
                    SortMenu(current = uiState.sort, onSortSelected = libraryViewModel::onSortChange)
                    IconButton(onClick = onNavigateToSettings, modifier = iconButtonSize) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings),
                            modifier = iconGlyphSize,
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (isAddRouteMode) {
                AddTracksFooter(
                    selectedCount = uiState.selectedIds.size,
                    onConfirm = {
                        val playlistId = addToPlaylistId ?: return@AddTracksFooter
                        // Fire the add + snackbar on the activity scope so they survive
                        // this screen disposing during the immediate popBackStack.
                        // Navigation runs first — the user sees the playlist instantly,
                        // not after the snackbar's 4-second timeout.
                        appScope.launch {
                            val added = libraryViewModel.addSelectedToPlaylist(playlistId)
                            if (added > 0) {
                                snackbarHostState.showSnackbar(tracksAddedFmt.format(added))
                            }
                        }
                        onNavigateBack()
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
                // Tied to what the inner list ACTUALLY scrolled, not what was requested. When
                // a tab's content fits the viewport (e.g. an album with three tracks),
                // `consumed.y` stays 0 so the header never collapses on dead overscroll. Reveal
                // and collapse are still proportional once the list has room to move.
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (headerHeightPx == 0 || consumed.y == 0f) return Offset.Zero
                    val newOffset = (headerOffsetPx.floatValue + consumed.y)
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
            val renderTab: @Composable (LibraryBrowseTab) -> Unit = { tab ->
                when (tab) {
                    LibraryBrowseTab.TRACKS -> TracksContent(
                        pagingItems = pagingItems,
                        uiState = uiState,
                        isAddRouteMode = isAddRouteMode,
                        audiobookOverrides = audiobookOverrides,
                        onTrackClick = { track ->
                            when {
                                isAddRouteMode || uiState.selectionMode ->
                                    libraryViewModel.toggleSelection(track)
                                else -> libraryViewModel.playLibraryQueueStartingFrom(track)
                            }
                        },
                        onTrackLongClick = libraryViewModel::toggleSelection,
                        onAddClick = { track -> pendingAddTrack = track },
                        onAudiobookToggle = libraryViewModel::toggleAudiobookOverride,
                        onTrackRenamed = libraryViewModel::onTrackRenamedLocally,
                        onTrackDeleted = libraryViewModel::onTrackDeletedLocally,
                        contentPadding = listContentPadding,
                    )
                    LibraryBrowseTab.ALBUMS -> {
                        // Click in selection mode toggles instead of navigating, mirroring
                        // the tracks-tab convention. Long-press always toggles.
                        val handleAlbumClick: (dev.maxxximgb.genesis.domain.model.Album) -> Unit = { album ->
                            if (uiState.albumSelectionMode) {
                                libraryViewModel.toggleAlbumSelection(album.id)
                            } else {
                                onNavigateToAlbum(album.id)
                            }
                        }
                        val handleAlbumLongClick: (dev.maxxximgb.genesis.domain.model.Album) -> Unit =
                            { album -> libraryViewModel.toggleAlbumSelection(album.id) }
                        if (uiState.albumsLayout == AlbumsLayout.GRID) {
                            AlbumsGrid(
                                albums = albums,
                                selectedIds = uiState.selectedAlbumIds,
                                onAlbumClick = handleAlbumClick,
                                onAlbumLongClick = handleAlbumLongClick,
                                contentPadding = listContentPadding,
                            )
                        } else {
                            AlbumsList(
                                albums = albums,
                                selectedIds = uiState.selectedAlbumIds,
                                onAlbumClick = handleAlbumClick,
                                onAlbumLongClick = handleAlbumLongClick,
                                contentPadding = listContentPadding,
                            )
                        }
                    }
                    LibraryBrowseTab.PLAYLISTS -> PlaylistsContent(
                        summaries = playlistSummaries,
                        selectedPlaylistIds = uiState.selectedPlaylistIds,
                        selectionMode = uiState.playlistSelectionMode,
                        onPlaylistClick = { id ->
                            if (uiState.playlistSelectionMode) {
                                libraryViewModel.togglePlaylistSelection(id)
                            } else {
                                onNavigateToPlaylist(id)
                            }
                        },
                        onPlaylistLongClick = libraryViewModel::togglePlaylistSelection,
                        onPlaylistRename = { pendingRenamePlaylist = it },
                        onPlaylistDelete = { pendingDeletePlaylist = it },
                        onPlaylistEnqueue = { id ->
                            scope.launch {
                                val n = libraryViewModel.enqueuePlaylist(id)
                                if (n > 0) {
                                    snackbarHostState.showSnackbar(addedToQueueFmt.format(n))
                                }
                            }
                        },
                        onCreate = { showCreatePlaylist = true },
                        contentPadding = listContentPadding,
                    )
                    LibraryBrowseTab.AUDIOBOOKS -> AudiobooksContent(
                        pagingItems = audiobookItems,
                        selectedIds = uiState.selectedIds,
                        selectionMode = uiState.selectionMode,
                        audiobookOverrides = audiobookOverrides,
                        onTrackClick = { track ->
                            if (uiState.selectionMode) libraryViewModel.toggleSelection(track)
                            else libraryViewModel.playAudiobookQueueStartingFrom(track)
                        },
                        onTrackLongClick = libraryViewModel::toggleSelection,
                        onAddClick = { track -> pendingAddTrack = track },
                        onAudiobookToggle = libraryViewModel::toggleAudiobookOverride,
                        onTrackRenamed = libraryViewModel::onTrackRenamedLocally,
                        onTrackDeleted = libraryViewModel::onTrackDeletedLocally,
                        contentPadding = listContentPadding,
                    )
                }
            }

            // Differentiates "tab tapped in segment control" (fade animation) from "pager
            // swiped" (slide animation). Hoisted at this Box level so both the pager block
            // and the segment-control overlay below share it via [handleTabSelect].
            var tapDrivenChange by remember { mutableStateOf(false) }
            val handleTabSelect: (LibraryBrowseTab) -> Unit = { tab ->
                if (tab != uiState.browseTab) {
                    tapDrivenChange = true
                    libraryViewModel.onBrowseTabChange(tab)
                }
            }

            val bypassPager = isAddRouteMode || uiState.searchQuery.isNotEmpty()
            // Pager state is hoisted out of the if/else so the segment control above can
            // read `pagerState.currentPage` and flip the chip highlight in sync with the
            // swipe — without this, the chip stays on the old tab until `settledPage`
            // fires (after the swipe finishes), which the user described as "laggy".
            val tabs = LibraryBrowseTab.entries
            val pagerState = rememberPagerState(
                initialPage = uiState.browseTab.ordinal,
                pageCount = { tabs.size },
            )
            if (bypassPager) {
                // Full-width tracks list — search results or add-to-playlist picker.
                renderTab(LibraryBrowseTab.TRACKS)
            } else {
                val fadeAlpha = remember { Animatable(1f) }

                // Tap-driven tab change → fade out, hard-snap the pager to the new page,
                // fade back in. Swipe-driven change is detected by the next LaunchedEffect
                // and routed through `tapDrivenChange = false`, so this branch is skipped.
                LaunchedEffect(uiState.browseTab) {
                    val target = uiState.browseTab.ordinal
                    if (pagerState.currentPage == target) return@LaunchedEffect
                    if (tapDrivenChange) {
                        fadeAlpha.animateTo(0f, animationSpec = tween(120))
                        pagerState.scrollToPage(target)
                        fadeAlpha.animateTo(1f, animationSpec = tween(160))
                        tapDrivenChange = false
                    }
                }
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.settledPage }.collect { page ->
                        val tab = tabs[page]
                        if (uiState.browseTab != tab) {
                            tapDrivenChange = false
                            libraryViewModel.onBrowseTabChange(tab)
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = fadeAlpha.value },
                    beyondBoundsPageCount = 1,
                    // Smoother snap so the swipe doesn't feel abrupt; default snaps via a
                    // stiff spring which felt too eager on the four-tab layout.
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapAnimationSpec = tween(durationMillis = 350),
                    ),
                ) { page ->
                    renderTab(tabs[page])
                }
            }

            // Header overlay (always full width, sliding via offset)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, headerOffsetPx.floatValue.toInt()) }
                    .onSizeChanged { headerHeightPx = it.height }
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                if (isAddRouteMode) {
                    // Add-tracks-to-playlist flow needs inline search to find tracks.
                    SearchField(
                        value = uiState.searchQuery,
                        onValueChange = libraryViewModel::onSearchQueryChange,
                        onSubmit = libraryViewModel::onSearchSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    )
                    if (uiState.searchQuery.isEmpty() && uiState.recentSearches.isNotEmpty()) {
                        SearchHistoryDropdown(
                            recent = uiState.recentSearches,
                            onClick = libraryViewModel::onRecentSearchClick,
                            onRemove = libraryViewModel::onRecentSearchRemove,
                        )
                    }
                } else {
                    // Slide chips up out of view while ANY selection mode is active —
                    // both track and playlist multi-select trigger the same chrome change.
                    AnimatedVisibility(
                        visible = !uiState.anySelectionMode,
                        enter = slideInVertically(tween(SELECTION_ANIM_MS)) { -it } +
                            fadeIn(tween(SELECTION_ANIM_MS)),
                        exit = slideOutVertically(tween(SELECTION_ANIM_MS)) { -it } +
                            fadeOut(tween(SELECTION_ANIM_MS)),
                    ) {
                        // Track the pager's currentPage so the chip highlight follows
                        // the swipe in real time. Falls back to uiState.browseTab in
                        // bypass mode (search / add-to-playlist) where the pager isn't
                        // mounted. derivedStateOf prevents recomposing on every pixel
                        // of pager scroll — only when currentPage actually flips.
                        val displayedTab by remember(bypassPager) {
                            derivedStateOf {
                                if (bypassPager) uiState.browseTab
                                else tabs[pagerState.currentPage]
                            }
                        }
                        LibrarySegmentControl(
                            current = displayedTab,
                            onSelect = handleTabSelect,
                        )
                    }
                }
            }
        }
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

    if (showAddSheetForAlbumSelection) {
        AddToPlaylistSheet(
            playlists = playlists,
            onSelect = { playlist ->
                scope.launch {
                    val added = libraryViewModel.addSelectedAlbumsToPlaylist(playlist.id)
                    if (added > 0) {
                        snackbarHostState.showSnackbar(tracksAddedFmt.format(added))
                    }
                }
                showAddSheetForAlbumSelection = false
            },
            onCreateNew = { showCreatePlaylist = true },
            onDismiss = { showAddSheetForAlbumSelection = false },
        )
    }

    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylist = false },
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    val pending = pendingAddTrack
                    val isTrackSelectionFlow = showAddSheetForSelection
                    val isAlbumSelectionFlow = showAddSheetForAlbumSelection
                    scope.launch {
                        val newId = libraryViewModel.createPlaylist(name)
                        when {
                            pending != null -> {
                                libraryViewModel.addSingleToPlaylist(pending, newId)
                                snackbarHostState.showSnackbar(trackAddedSingular)
                            }
                            isTrackSelectionFlow -> {
                                val added = libraryViewModel.addSelectedToPlaylist(newId)
                                if (added > 0) {
                                    snackbarHostState.showSnackbar(tracksAddedFmt.format(added))
                                }
                            }
                            isAlbumSelectionFlow -> {
                                val added = libraryViewModel.addSelectedAlbumsToPlaylist(newId)
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
                showAddSheetForAlbumSelection = false
            },
        )
    }

    val pendingRename = pendingRenamePlaylist
    if (pendingRename != null) {
        dev.maxxximgb.genesis.ui.playlists.components.RenamePlaylistDialog(
            initial = pendingRename.name,
            onDismiss = { pendingRenamePlaylist = null },
            onConfirm = { newName ->
                if (newName.isNotBlank() && newName != pendingRename.name) {
                    scope.launch {
                        libraryViewModel.renamePlaylist(pendingRename.id, newName)
                        snackbarHostState.showSnackbar(playlistRenamedMsg)
                    }
                }
                pendingRenamePlaylist = null
            },
        )
    }

    val pendingDelete = pendingDeletePlaylist
    if (pendingDelete != null) {
        dev.maxxximgb.genesis.ui.playlists.components.DeletePlaylistDialog(
            playlistName = pendingDelete.name,
            onDismiss = { pendingDeletePlaylist = null },
            onConfirm = {
                scope.launch {
                    libraryViewModel.deletePlaylist(pendingDelete.id)
                    snackbarHostState.showSnackbar(playlistDeletedMsg)
                }
                pendingDeletePlaylist = null
            },
        )
    }

    if (showBulkDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_playlist_title)) },
            text = { Text(stringResource(R.string.delete_playlist_message)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showBulkDeleteConfirm = false
                    scope.launch {
                        val n = libraryViewModel.deleteSelectedPlaylists()
                        if (n > 0) snackbarHostState.showSnackbar(playlistDeletedMsg)
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
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
            .height(Sizes.searchFieldHeight)
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
    audiobookOverrides: Set<Long>,
    onTrackClick: (Track) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onAddClick: (Track) -> Unit,
    onAudiobookToggle: (Long) -> Unit,
    onTrackRenamed: (Long, String) -> Unit = { _, _ -> },
    onTrackDeleted: (Long) -> Unit = {},
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
            audiobookOverrides = audiobookOverrides,
            contentPadding = contentPadding,
            onTrackClick = onTrackClick,
            onTrackLongClick = onTrackLongClick,
            onAddClick = onAddClick,
            onAudiobookToggle = onAudiobookToggle,
            onTrackRenamed = onTrackRenamed,
            onTrackDeleted = onTrackDeleted,
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TrackList(
    items: LazyPagingItems<Track>,
    selectedIds: Set<Long>,
    addRouteMode: Boolean,
    currentMediaStoreId: Long?,
    audiobookOverrides: Set<Long>,
    onTrackClick: (Track) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onAddClick: (Track) -> Unit,
    onAudiobookToggle: (Long) -> Unit,
    onTrackRenamed: (Long, String) -> Unit = { _, _ -> },
    onTrackDeleted: (Long) -> Unit = {},
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(0.dp),
) {
    val addLabel = stringResource(R.string.add_to_playlist)
    val markAudiobookLabel = stringResource(R.string.mark_as_audiobook)
    val unmarkAudiobookLabel = stringResource(R.string.unmark_audiobook)
    // Per-list mutator: registers an IntentSender launcher and hosts the
    // delete-confirm / rename dialogs so each track's overflow menu can fire
    // delete + rename without each calling screen plumbing the consent flow.
    // Callbacks let the host (e.g. LibraryViewModel) layer a session overlay so
    // the row updates / disappears in place — without rebuilding the pager and
    // snapping the scroll to the top.
    val mutator = dev.maxxximgb.genesis.ui.components.rememberTrackMutator(
        onTrackRenamed = onTrackRenamed,
        onTrackDeleted = onTrackDeleted,
    )

    val selectionMode = addRouteMode || selectedIds.isNotEmpty()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    // Snapshots so the gesture handler sees the latest selection set + paged items
    // even though pointerInput's lambda is captured once.
    val currentSelection = androidx.compose.runtime.rememberUpdatedState(selectedIds)
    val currentItems = androidx.compose.runtime.rememberUpdatedState(items)
    val currentToggle = androidx.compose.runtime.rememberUpdatedState(onTrackLongClick)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Custom long-press → drag-select with auto-scroll. The standard
                // detectDragGesturesAfterLongPress reports onDragStart at the position WHERE
                // long-press fired (after up to long-press timeout of finger drift), which
                // produces off-by-one selections when the finger drifts during the wait. We
                // capture the initial DOWN position and use that as the first toggled row.
                val edgeZonePx = with(density) { 80.dp.toPx() }
                val maxScrollPxPerFrame = with(density) { 14.dp.toPx() }
                val viewportHeight = size.height.toFloat()

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val initialY = down.position.y

                    // awaitLongPressOrCancellation returns null if the pointer is released or
                    // drags past slop before timeout (i.e. it's a tap or a scroll). We use the
                    // INITIAL down Y, not the position the long-press lambda reports — the
                    // finger can drift up to slop during the wait, which produced an
                    // off-by-one selection with detectDragGesturesAfterLongPress.
                    awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture

                    val firstTrack = trackAtY(listState, currentItems.value, initialY)
                        ?: return@awaitEachGesture
                    val wasSelected = firstTrack.mediaStoreId in currentSelection.value
                    val target = !wasSelected
                    currentToggle.value(firstTrack)
                    val visited = mutableSetOf(firstTrack.mediaStoreId)

                    // Auto-scroll job: while finger sits in the top/bottom edge zone, the list
                    // scrolls in that direction and we keep checking which row is now under
                    // the (stationary) finger so newly-revealed rows get toggled too.
                    var currentY: Float = initialY
                    val scrollJob = coroutineScope.launch {
                        while (isActive) {
                            val y = currentY
                            val speed = when {
                                y < edgeZonePx ->
                                    -maxScrollPxPerFrame *
                                        ((edgeZonePx - y) / edgeZonePx).coerceIn(0f, 1f)
                                y > viewportHeight - edgeZonePx ->
                                    maxScrollPxPerFrame *
                                        ((y - (viewportHeight - edgeZonePx)) / edgeZonePx)
                                            .coerceIn(0f, 1f)
                                else -> 0f
                            }
                            if (speed != 0f) listState.scrollBy(speed)
                            val t = trackAtY(listState, currentItems.value, y)
                            if (t != null && t.mediaStoreId !in visited) {
                                visited += t.mediaStoreId
                                val isSel = t.mediaStoreId in currentSelection.value
                                if (isSel != target) currentToggle.value(t)
                            }
                            kotlinx.coroutines.delay(16)
                        }
                    }

                    try {
                        verticalDrag(down.id) { change ->
                            change.consume()
                            currentY = change.position.y
                        }
                    } finally {
                        scrollJob.cancel()
                    }
                }
            },
        state = listState,
        contentPadding = contentPadding,
    ) {
        items(
            count = items.itemCount,
            key = items.itemKey { it.mediaStoreId },
        ) { index ->
            val track = items[index] ?: return@items
            val selected = track.mediaStoreId in selectedIds
            val isCurrent = currentMediaStoreId != null && track.mediaStoreId == currentMediaStoreId
            val isAudiobook = track.mediaStoreId in audiobookOverrides
            TrackRow(
                // animateItemPlacement so neighbouring rows slide up smoothly when the
                // user deletes a track (the row itself disappears immediately when its
                // mediaStoreId joins pendingDeletes; the rows below tween into the
                // freed-up vertical space).
                modifier = Modifier.animateItemPlacement(),
                track = track,
                selected = selected,
                selectionMode = selectionMode,
                isCurrentlyPlaying = isCurrent,
                onClick = { onTrackClick(track) },
                // Long-press handled by parent's detectDragGesturesAfterLongPress so the SAME
                // gesture can extend to drag-select. Forwarding to onTrackLongClick here would
                // double-toggle the first row.
                onLongClick = {},
                actions = if (!selectionMode) {
                    listOf(
                        TrackAction(
                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                            label = addLabel,
                            onClick = { onAddClick(track) },
                        ),
                        TrackAction(
                            icon = if (isAudiobook) Icons.Filled.BookmarkRemove
                            else Icons.Filled.BookmarkAdd,
                            label = if (isAudiobook) unmarkAudiobookLabel else markAudiobookLabel,
                            onClick = { onAudiobookToggle(track.mediaStoreId) },
                        ),
                        mutator.renameAction(track),
                        mutator.deleteAction(track),
                    )
                } else emptyList(),
            )
        }
    }
}

private fun trackAtY(
    state: androidx.compose.foundation.lazy.LazyListState,
    items: LazyPagingItems<Track>,
    y: Float,
): Track? {
    // LazyListItemInfo.offset is in viewport-content coordinates (0 = top of the content
    // area, i.e. AFTER beforeContentPadding), while `y` is the pointerInput coord on the
    // LazyColumn (0 = top of LazyColumn modifier bounds). They differ by beforeContentPadding,
    // which equals -viewportStartOffset. Convert the pointer Y into scroll-space so the
    // lookup hits the row the user actually touched.
    val viewportStart = state.layoutInfo.viewportStartOffset
    val scrollY = y + viewportStart
    val info = state.layoutInfo.visibleItemsInfo.firstOrNull {
        scrollY >= it.offset && scrollY < it.offset + it.size
    } ?: return null
    return items.peek(info.index)
}

@Composable
private fun PlaylistsContent(
    summaries: List<dev.maxxximgb.genesis.domain.model.PlaylistSummary>,
    selectedPlaylistIds: Set<Long>,
    selectionMode: Boolean,
    onPlaylistClick: (Long) -> Unit,
    onPlaylistLongClick: (Long) -> Unit,
    onPlaylistRename: (dev.maxxximgb.genesis.domain.model.Playlist) -> Unit,
    onPlaylistDelete: (dev.maxxximgb.genesis.domain.model.Playlist) -> Unit,
    onPlaylistEnqueue: (Long) -> Unit,
    onCreate: () -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(0.dp),
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val edgeZonePx = with(density) { 80.dp.toPx() }
    val maxScrollPxPerFrame = with(density) { 14.dp.toPx() }
    val currentSelection = androidx.compose.runtime.rememberUpdatedState(selectedPlaylistIds)
    val currentToggle = androidx.compose.runtime.rememberUpdatedState(onPlaylistLongClick)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Lookup uses LazyListItemInfo.key (Long); only PlaylistCard items have a
                // Long key, so the leading "create-playlist" button + empty-state filter
                // out naturally.
                val viewportHeight = size.height.toFloat()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val initialY = down.position.y
                    awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    val firstId = playlistAtY(listState, initialY) ?: return@awaitEachGesture
                    val wasSelected = firstId in currentSelection.value
                    val target = !wasSelected
                    currentToggle.value(firstId)
                    val visited = mutableSetOf(firstId)

                    var currentY: Float = initialY
                    val scrollJob = scope.launch {
                        while (isActive) {
                            val y = currentY
                            val speed = when {
                                y < edgeZonePx ->
                                    -maxScrollPxPerFrame *
                                        ((edgeZonePx - y) / edgeZonePx).coerceIn(0f, 1f)
                                y > viewportHeight - edgeZonePx ->
                                    maxScrollPxPerFrame *
                                        ((y - (viewportHeight - edgeZonePx)) / edgeZonePx)
                                            .coerceIn(0f, 1f)
                                else -> 0f
                            }
                            if (speed != 0f) listState.scrollBy(speed)
                            val id = playlistAtY(listState, y)
                            if (id != null && id !in visited) {
                                visited += id
                                val isSel = id in currentSelection.value
                                if (isSel != target) currentToggle.value(id)
                            }
                            delay(16)
                        }
                    }

                    try {
                        verticalDrag(down.id) { change ->
                            change.consume()
                            currentY = change.position.y
                        }
                    } finally {
                        scrollJob.cancel()
                    }
                }
            },
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Hide the create button while bulk-selecting — top bar carries the bulk action and
        // the button would just be visual noise.
        if (!selectionMode) {
            item(key = "create-playlist") {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs)) {
                    Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(
                            text = stringResource(R.string.new_playlist),
                            modifier = Modifier.padding(start = Spacing.sm),
                        )
                    }
                }
            }
        }
        if (summaries.isEmpty()) {
            item(key = "empty-state") {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    title = stringResource(R.string.empty_playlists_title),
                    subtitle = stringResource(R.string.empty_playlists_subtitle),
                )
            }
        } else {
            items(
                count = summaries.size,
                key = { summaries[it].playlist.id },
            ) { index ->
                val summary = summaries[index]
                val playlist = summary.playlist
                Box(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                    dev.maxxximgb.genesis.ui.components.PlaylistCard(
                        playlist = playlist,
                        trackCount = summary.trackCount,
                        selected = playlist.id in selectedPlaylistIds,
                        selectionMode = selectionMode,
                        onClick = { onPlaylistClick(playlist.id) },
                        // Long-press handled by the parent's pointerInput so the SAME gesture
                        // can extend into drag-select. Forwarding here would double-toggle.
                        onLongClick = {},
                        onRename = { onPlaylistRename(playlist) },
                        onDelete = { onPlaylistDelete(playlist) },
                        onEnqueue = { onPlaylistEnqueue(playlist.id) },
                    )
                }
            }
        }
    }
}

private fun playlistAtY(state: LazyListState, y: Float): Long? {
    // Pointer Y → scroll-content Y. PlaylistCard items use the playlist id (Long) as
    // their LazyListItemInfo.key; "create-playlist" and "empty-state" use String keys
    // and naturally drop out of the cast.
    val scrollY = y + state.layoutInfo.viewportStartOffset
    val info = state.layoutInfo.visibleItemsInfo.firstOrNull {
        scrollY >= it.offset && scrollY < it.offset + it.size
    } ?: return null
    return info.key as? Long
}

@Composable
private fun AudiobooksContent(
    pagingItems: LazyPagingItems<Track>,
    selectedIds: Set<Long>,
    selectionMode: Boolean,
    audiobookOverrides: Set<Long>,
    onTrackClick: (Track) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onAddClick: (Track) -> Unit,
    onAudiobookToggle: (Long) -> Unit,
    onTrackRenamed: (Long, String) -> Unit = { _, _ -> },
    onTrackDeleted: (Long) -> Unit = {},
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(0.dp),
) {
    when {
        pagingItems.loadState.refresh is LoadState.Loading -> LoadingState()
        pagingItems.itemCount == 0 -> EmptyState(
            icon = Icons.Filled.LibraryMusic,
            title = stringResource(R.string.empty_audiobooks_title),
            subtitle = stringResource(R.string.empty_audiobooks_subtitle),
        )
        else -> TrackList(
            items = pagingItems,
            selectedIds = selectedIds,
            addRouteMode = false,
            currentMediaStoreId = null,
            audiobookOverrides = audiobookOverrides,
            contentPadding = contentPadding,
            onTrackClick = onTrackClick,
            onTrackLongClick = onTrackLongClick,
            onAddClick = onAddClick,
            onAudiobookToggle = onAudiobookToggle,
            onTrackRenamed = onTrackRenamed,
            onTrackDeleted = onTrackDeleted,
        )
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
