package dev.maxxximgb.genesis.ui.library

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.ui.components.LoadingState
import dev.maxxximgb.genesis.ui.components.TrackAction
import dev.maxxximgb.genesis.ui.components.TrackRow
import dev.maxxximgb.genesis.ui.components.rememberTrackMutator
import dev.maxxximgb.genesis.ui.library.components.AddToPlaylistSheet
import dev.maxxximgb.genesis.ui.nowPlaying.NowPlayingViewModel
import dev.maxxximgb.genesis.ui.playlists.components.CreatePlaylistDialog
import dev.maxxximgb.genesis.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val album by viewModel.album.collectAsStateWithLifecycle()
    val items = viewModel.pagedTracks.collectAsLazyPagingItems()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val audiobookOverrides by viewModel.audiobookOverrides.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showAddAllSheet by remember { mutableStateOf(false) }
    var showAddSelectedSheet by remember { mutableStateOf(false) }
    // Single-track "add to playlist" flow driven by the per-row kebab menu. Holds the
    // tapped track until the user picks a playlist (or cancels).
    var pendingAddTrack by remember { mutableStateOf<Track?>(null) }
    // Three create-dialog modes: from "add all", from "add selected", or from a single
    // track's kebab. The non-null pendingCreateForTrack wins; otherwise createForSelection
    // distinguishes the other two.
    var showCreateDialog by remember { mutableStateOf(false) }
    var createForSelection by remember { mutableStateOf(false) }
    var pendingCreateForTrack by remember { mutableStateOf<Track?>(null) }
    val tracksAddedFmt = stringResource(R.string.snack_tracks_added)
    val markedAudiobookFmt = stringResource(R.string.snack_marked_audiobooks)
    val addLabel = stringResource(R.string.add_to_playlist)
    val markAudiobookLabel = stringResource(R.string.mark_as_audiobook)
    val unmarkAudiobookLabel = stringResource(R.string.unmark_audiobook)
    val mutator = rememberTrackMutator(
        onTrackRenamed = viewModel::onTrackRenamedLocally,
        onTrackDeleted = viewModel::onTrackDeletedLocally,
    )

    BrowseDetail(
        title = album?.name ?: "",
        subtitle = album?.let { stringResource(R.string.album_track_count, it.trackCount) },
        onNavigateBack = onNavigateBack,
        items = items,
        onPlay = nowPlayingViewModel::playTracks,
        onAddAllToPlaylist = { showAddAllSheet = true },
        onMarkAllAsAudiobook = {
            scope.launch {
                val n = viewModel.markAllAsAudiobook()
                if (n > 0) snackbarHostState.showSnackbar(markedAudiobookFmt.format(n))
            }
        },
        selectedIds = selectedIds,
        onTrackLongClick = viewModel::toggleTrackSelection,
        onClearSelection = viewModel::clearTrackSelection,
        onAddSelectedToPlaylist = { showAddSelectedSheet = true },
        trackActions = { track ->
            val isAudiobook = track.mediaStoreId in audiobookOverrides
            listOf(
                TrackAction(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    label = addLabel,
                    onClick = { pendingAddTrack = track },
                ),
                TrackAction(
                    icon = if (isAudiobook) Icons.Filled.BookmarkRemove
                    else Icons.Filled.BookmarkAdd,
                    label = if (isAudiobook) unmarkAudiobookLabel else markAudiobookLabel,
                    onClick = { viewModel.toggleAudiobookOverride(track.mediaStoreId) },
                ),
                mutator.renameAction(track),
                mutator.deleteAction(track),
            )
        },
    )

    pendingAddTrack?.let { track ->
        AddToPlaylistSheet(
            playlists = playlists,
            onSelect = { playlist ->
                scope.launch {
                    viewModel.addSingleToPlaylist(track, playlist.id)
                    snackbarHostState.showSnackbar(tracksAddedFmt.format(1))
                }
                pendingAddTrack = null
            },
            onCreateNew = {
                pendingCreateForTrack = track
                pendingAddTrack = null
                showCreateDialog = true
            },
            onDismiss = { pendingAddTrack = null },
        )
    }

    if (showAddAllSheet) {
        AddToPlaylistSheet(
            playlists = playlists,
            onSelect = { playlist ->
                scope.launch {
                    val n = viewModel.addAllToPlaylist(playlist.id)
                    if (n > 0) snackbarHostState.showSnackbar(tracksAddedFmt.format(n))
                }
                showAddAllSheet = false
            },
            onCreateNew = {
                showAddAllSheet = false
                createForSelection = false
                showCreateDialog = true
            },
            onDismiss = { showAddAllSheet = false },
        )
    }

    if (showAddSelectedSheet) {
        AddToPlaylistSheet(
            playlists = playlists,
            onSelect = { playlist ->
                scope.launch {
                    val n = viewModel.addSelectedToPlaylist(playlist.id)
                    if (n > 0) snackbarHostState.showSnackbar(tracksAddedFmt.format(n))
                }
                showAddSelectedSheet = false
            },
            onCreateNew = {
                showAddSelectedSheet = false
                createForSelection = true
                showCreateDialog = true
            },
            onDismiss = { showAddSelectedSheet = false },
        )
    }

    if (showCreateDialog) {
        val isForSelection = createForSelection
        val singleTrack = pendingCreateForTrack
        CreatePlaylistDialog(
            onDismiss = {
                showCreateDialog = false
                pendingCreateForTrack = null
            },
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    scope.launch {
                        if (singleTrack != null) {
                            viewModel.addSingleToNewPlaylist(singleTrack, name)
                            snackbarHostState.showSnackbar(tracksAddedFmt.format(1))
                        } else {
                            val n = if (isForSelection) viewModel.addSelectedToNewPlaylist(name)
                            else viewModel.createAndAddAll(name)
                            if (n > 0) snackbarHostState.showSnackbar(tracksAddedFmt.format(n))
                        }
                    }
                }
                showCreateDialog = false
                createForSelection = false
                pendingCreateForTrack = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") snackbarHostState: SnackbarHostState,
    viewModel: ArtistDetailViewModel = hiltViewModel(),
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val items = viewModel.pagedTracks.collectAsLazyPagingItems()
    BrowseDetail(
        title = artist?.name ?: "",
        subtitle = artist?.let { stringResource(R.string.artist_track_count, it.trackCount, it.albumCount) },
        onNavigateBack = onNavigateBack,
        items = items,
        onPlay = nowPlayingViewModel::playTracks,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") snackbarHostState: SnackbarHostState,
    viewModel: FolderDetailViewModel = hiltViewModel(),
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val folder by viewModel.folder.collectAsStateWithLifecycle()
    val items = viewModel.pagedTracks.collectAsLazyPagingItems()
    BrowseDetail(
        title = folder?.displayName ?: "",
        subtitle = folder?.let { stringResource(R.string.folder_track_count, it.trackCount) },
        onNavigateBack = onNavigateBack,
        items = items,
        onPlay = nowPlayingViewModel::playTracks,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseDetail(
    title: String,
    subtitle: String?,
    onNavigateBack: () -> Unit,
    items: LazyPagingItems<Track>,
    onPlay: (List<Track>, Int) -> Unit,
    onAddAllToPlaylist: (() -> Unit)? = null,
    onMarkAllAsAudiobook: (() -> Unit)? = null,
    // Selection extensions — only AlbumDetailScreen wires these. Artist/Folder pass nothing
    // so the screen falls back to the original simple list behavior.
    selectedIds: Set<Long> = emptySet(),
    onTrackLongClick: ((Track) -> Unit)? = null,
    onClearSelection: () -> Unit = {},
    onAddSelectedToPlaylist: () -> Unit = {},
    // Per-row kebab menu — only AlbumDetailScreen wires this. Suppressed automatically
    // when selection mode is on so the chevron column stays usable as a tap target.
    trackActions: (Track) -> List<TrackAction> = { emptyList() },
) {
    val selectionMode = selectedIds.isNotEmpty()
    Scaffold(
        // Compact inline header — replaces M3 TopAppBar's ~64dp reservation. The Row sizes
        // to its content; only the system status-bar inset eats vertical space at the top.
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectionMode) {
                    IconButton(onClick = onClearSelection) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                    Text(
                        text = stringResource(R.string.selected_count, selectedIds.size),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = Spacing.sm),
                    )
                    IconButton(onClick = onAddSelectedToPlaylist) {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = stringResource(R.string.add_to_playlist),
                        )
                    }
                } else {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(end = Spacing.sm)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (onAddAllToPlaylist != null && items.itemCount > 0) {
                        IconButton(onClick = onAddAllToPlaylist) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = stringResource(R.string.add_to_playlist),
                            )
                        }
                    }
                    if (onMarkAllAsAudiobook != null && items.itemCount > 0) {
                        IconButton(onClick = onMarkAllAsAudiobook) {
                            Icon(
                                Icons.Filled.MenuBook,
                                contentDescription = stringResource(R.string.mark_as_audiobook),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (items.itemCount == 0) {
                LoadingState()
            } else {
                val listState = rememberLazyListState()
                val density = LocalDensity.current
                val scope = rememberCoroutineScope()
                val edgeZonePx = with(density) { 80.dp.toPx() }
                val maxScrollPxPerFrame = with(density) { 14.dp.toPx() }
                val currentItems = rememberUpdatedState(items)
                val currentSelection = rememberUpdatedState(selectedIds)
                val currentToggle = rememberUpdatedState(onTrackLongClick)

                val gestureModifier = if (onTrackLongClick != null) {
                    Modifier.pointerInput(Unit) {
                        val viewportHeight = size.height.toFloat()
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val initialY = down.position.y
                            awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture

                            val first = trackAtY(listState, currentItems.value, initialY)
                                ?: return@awaitEachGesture
                            val toggle = currentToggle.value ?: return@awaitEachGesture
                            val wasSelected = first.mediaStoreId in currentSelection.value
                            val target = !wasSelected
                            toggle(first)
                            val visited = mutableSetOf(first.mediaStoreId)

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
                                    val t = trackAtY(listState, currentItems.value, y)
                                    if (t != null && t.mediaStoreId !in visited) {
                                        visited += t.mediaStoreId
                                        val isSel = t.mediaStoreId in currentSelection.value
                                        if (isSel != target) currentToggle.value?.invoke(t)
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
                    }
                } else Modifier
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = Spacing.xs)
                        .then(gestureModifier),
                    state = listState,
                ) {
                    items(
                        count = items.itemCount,
                        key = items.itemKey { it.mediaStoreId },
                    ) { index ->
                        val track = items[index] ?: return@items
                        val isSelected = track.mediaStoreId in selectedIds
                        TrackRow(
                            track = track,
                            selected = isSelected,
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) {
                                    onTrackLongClick?.invoke(track)
                                } else {
                                    val snapshot = items.itemSnapshotList.items
                                    val idx = snapshot.indexOfFirst {
                                        it.mediaStoreId == track.mediaStoreId
                                    }
                                    if (idx >= 0) onPlay(snapshot, idx)
                                }
                            },
                            // Long-press handled by the parent's pointerInput so the SAME
                            // gesture extends to drag-select. Forwarding here would double-toggle.
                            onLongClick = {},
                            actions = if (selectionMode) emptyList() else trackActions(track),
                        )
                    }
                }
            }
        }
    }
}

private fun trackAtY(
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
