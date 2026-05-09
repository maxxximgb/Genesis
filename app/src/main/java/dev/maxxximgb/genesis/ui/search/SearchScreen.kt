package dev.maxxximgb.genesis.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
import dev.maxxximgb.genesis.ui.library.components.SearchHistoryDropdown
import dev.maxxximgb.genesis.ui.nowPlaying.NowPlayingViewModel
import dev.maxxximgb.genesis.ui.playlists.components.CreatePlaylistDialog
import dev.maxxximgb.genesis.ui.theme.Sizes
import dev.maxxximgb.genesis.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: SearchViewModel = hiltViewModel(),
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val recent by viewModel.recentSearches.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val audiobookOverrides by viewModel.audiobookOverrides.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagedResults.collectAsLazyPagingItems()
    val playbackState by nowPlayingViewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val selectionMode = selectedIds.isNotEmpty()

    var pendingAddTrack by remember { mutableStateOf<Track?>(null) }
    var showAddSheetForSelection by remember { mutableStateOf(false) }
    var showCreatePlaylist by remember { mutableStateOf(false) }

    val tracksAddedFmt = stringResource(R.string.snack_tracks_added)
    val trackAddedSingular = stringResource(R.string.snack_track_added)
    val markedAudiobookFmt = stringResource(R.string.snack_marked_audiobooks)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text(stringResource(R.string.selected_count, selectedIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cancel),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddSheetForSelection = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = stringResource(R.string.add_to_playlist),
                            )
                        }
                        IconButton(onClick = {
                            scope.launch {
                                val count = viewModel.markSelectedAsAudiobooks()
                                if (count > 0) {
                                    snackbarHostState.showSnackbar(markedAudiobookFmt.format(count))
                                }
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = stringResource(R.string.mark_as_audiobook),
                            )
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        SearchField(
                            value = query,
                            onValueChange = viewModel::onQueryChange,
                            onSubmit = viewModel::onSubmit,
                            focusRequester = focusRequester,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                query.isBlank() -> {
                    if (recent.isNotEmpty()) {
                        SearchHistoryDropdown(
                            recent = recent,
                            onClick = viewModel::onRecentClick,
                            onRemove = viewModel::onRecentRemove,
                        )
                    }
                }
                pagingItems.loadState.refresh is LoadState.Loading -> LoadingState()
                pagingItems.itemCount == 0 -> EmptyState(
                    icon = Icons.Filled.Search,
                    title = stringResource(R.string.empty_search_title),
                    subtitle = stringResource(R.string.empty_search_subtitle),
                )
                else -> {
                    val addLabel = stringResource(R.string.add_to_playlist)
                    val markAudiobookLabel = stringResource(R.string.mark_as_audiobook)
                    val unmarkAudiobookLabel = stringResource(R.string.unmark_audiobook)
                    val listState = rememberLazyListState()
                    val currentSelection = rememberUpdatedState(selectedIds)
                    val currentItems = rememberUpdatedState(pagingItems)
                    val currentToggle = rememberUpdatedState<(Track) -> Unit> {
                        viewModel.toggleSelection(it)
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                val edgeZonePx = with(density) { 80.dp.toPx() }
                                val maxScrollPxPerFrame = with(density) { 14.dp.toPx() }
                                val viewportHeight = size.height.toFloat()

                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val initialY = down.position.y
                                    awaitLongPressOrCancellation(down.id)
                                        ?: return@awaitEachGesture

                                    val firstTrack = searchTrackAtY(
                                        listState,
                                        currentItems.value,
                                        initialY,
                                    ) ?: return@awaitEachGesture
                                    val target = firstTrack.mediaStoreId !in currentSelection.value
                                    currentToggle.value(firstTrack)
                                    val visited = mutableSetOf(firstTrack.mediaStoreId)

                                    var currentY = initialY
                                    val scrollJob = scope.launch {
                                        while (isActive) {
                                            val y = currentY
                                            val speed = when {
                                                y < edgeZonePx -> -maxScrollPxPerFrame *
                                                    ((edgeZonePx - y) / edgeZonePx)
                                                        .coerceIn(0f, 1f)
                                                y > viewportHeight - edgeZonePx ->
                                                    maxScrollPxPerFrame *
                                                        ((y - (viewportHeight - edgeZonePx)) /
                                                            edgeZonePx).coerceIn(0f, 1f)
                                                else -> 0f
                                            }
                                            if (speed != 0f) listState.scrollBy(speed)
                                            val track = searchTrackAtY(
                                                listState,
                                                currentItems.value,
                                                y,
                                            )
                                            if (track != null && track.mediaStoreId !in visited) {
                                                visited += track.mediaStoreId
                                                val isSelectedNow =
                                                    track.mediaStoreId in currentSelection.value
                                                if (isSelectedNow != target) {
                                                    currentToggle.value(track)
                                                }
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
                    ) {
                        items(
                            count = pagingItems.itemCount,
                            key = pagingItems.itemKey { it.mediaStoreId },
                        ) { index ->
                            val track = pagingItems[index] ?: return@items
                            val isCurrent = playbackState.currentMediaStoreId == track.mediaStoreId
                            val isSelected = track.mediaStoreId in selectedIds
                            val isAudiobook = track.mediaStoreId in audiobookOverrides
                            TrackRow(
                                track = track,
                                selected = isSelected,
                                selectionMode = selectionMode,
                                isCurrentlyPlaying = isCurrent,
                                onClick = {
                                    if (selectionMode) viewModel.toggleSelection(track)
                                    else nowPlayingViewModel.playSingle(track)
                                },
                                onLongClick = {},
                                actions = if (!selectionMode) {
                                    listOf(
                                        TrackAction(
                                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                                            label = addLabel,
                                            onClick = { pendingAddTrack = track },
                                        ),
                                        TrackAction(
                                            icon = if (isAudiobook) Icons.Filled.BookmarkRemove
                                            else Icons.Filled.BookmarkAdd,
                                            label = if (isAudiobook) unmarkAudiobookLabel
                                            else markAudiobookLabel,
                                            onClick = {
                                                viewModel.toggleAudiobookOverride(track.mediaStoreId)
                                            },
                                        ),
                                    )
                                } else emptyList(),
                            )
                        }
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
                    viewModel.addSingleToPlaylist(pendingTrack, playlist.id)
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
                    val added = viewModel.addSelectedToPlaylist(playlist.id)
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
                        val newId = viewModel.createPlaylist(name)
                        when {
                            pending != null -> {
                                viewModel.addSingleToPlaylist(pending, newId)
                                snackbarHostState.showSnackbar(trackAddedSingular)
                            }
                            isSelectionFlow -> {
                                val added = viewModel.addSelectedToPlaylist(newId)
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

private fun searchTrackAtY(
    state: LazyListState,
    items: LazyPagingItems<Track>,
    y: Float,
): Track? {
    val scrollY = y + state.layoutInfo.viewportStartOffset
    val info = state.layoutInfo.visibleItemsInfo.firstOrNull {
        scrollY >= it.offset && scrollY < it.offset + it.size
    } ?: return null
    return items.peek(info.index)
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val textStyle = LocalTextStyle.current.merge(
        MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
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
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(LocalContentColor.current),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
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
