package dev.maxxximgb.genesis.ui.playlistDetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.maxxximgb.genesis.domain.model.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.ui.components.EmptyState
import dev.maxxximgb.genesis.ui.components.LoadingState
import dev.maxxximgb.genesis.ui.components.TrackAction
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
    val markedAudiobookFmt = stringResource(R.string.snack_marked_audiobooks)

    // Hoisted so the FAB (in Scaffold's slot) can read scroll position. We hide the FAB
    // once the inline "Add tracks" pill at the bottom of the list scrolls into view, so
    // there's exactly one add-affordance on screen at a time (matches the YM pattern the
    // user pointed at).
    val listState = rememberLazyListState()
    val tracksCount = (state as? PlaylistDetailUiState.Content)?.detail?.tracks?.size ?: 0
    val addButtonVisible by remember(tracksCount) {
        derivedStateOf {
            if (tracksCount == 0) return@derivedStateOf false
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            // Footer item is at index == tracksCount (after items 0..tracksCount-1).
            last.index >= tracksCount
        }
    }

    Scaffold(
        topBar = {
            // Compact inline header — replaces M3 TopAppBar's ~64dp reservation. The Row
            // sizes to its content so only the system status-bar inset eats vertical space.
            when (val s = state) {
                is PlaylistDetailUiState.Content -> {
                    if (s.selectionMode) {
                        CompactHeaderRow(
                            navIcon = {
                                IconButton(onClick = viewModel::clearSelection) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cancel),
                                    )
                                }
                            },
                            title = stringResource(R.string.selected_count, s.selectedIds.size),
                        )
                    } else if (s.reorderMode) {
                        CompactHeaderRow(
                            navIcon = {
                                IconButton(onClick = { viewModel.setReorderMode(false) }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cancel),
                                    )
                                }
                            },
                            title = stringResource(R.string.reorder_tracks),
                        )
                    } else {
                        CompactHeaderRow(
                            navIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        Icons.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back),
                                    )
                                }
                            },
                            title = s.detail.playlist.name,
                            actions = {
                                if (s.detail.tracks.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setReorderMode(true) }) {
                                        Icon(
                                            imageVector = Icons.Filled.SwapVert,
                                            contentDescription = stringResource(R.string.reorder_tracks),
                                        )
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            val n = viewModel.markAllAsAudiobook()
                                            if (n > 0) {
                                                snackbarHostState.showSnackbar(
                                                    markedAudiobookFmt.format(n),
                                                )
                                            }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.MenuBook,
                                            contentDescription = stringResource(R.string.mark_as_audiobook),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }

                else -> CompactHeaderRow(
                    navIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    title = stringResource(R.string.playlists_title),
                )
            }
        },
        floatingActionButton = {
            (state as? PlaylistDetailUiState.Content)?.let { content ->
                if (!content.selectionMode && !content.reorderMode && !addButtonVisible) {
                    SmallFloatingActionButton(
                        onClick = { onAddTracks(viewModel.playlistId) },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_tracks),
                        )
                    }
                }
            }
        },
        bottomBar = {
            (state as? PlaylistDetailUiState.Content)?.takeIf { it.selectionMode }?.let { content ->
                val tracks = content.detail.tracks
                val selectedIndices = tracks.withIndex()
                    .filter { it.value.mediaStoreId in content.selectedIds }
                    .map { it.index }
                val canMoveUp = selectedIndices.isNotEmpty() && selectedIndices.min() > 0
                val canMoveDown = selectedIndices.isNotEmpty() &&
                    selectedIndices.max() < tracks.lastIndex
                SelectionActionBar(
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
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
                        val removeLabel = stringResource(R.string.remove_from_playlist)
                        val dragHandleLabel = stringResource(R.string.cd_drag_to_reorder)
                        // MediaStore-level delete + rename actions (consent-prompt + dialog).
                        val mutator = dev.maxxximgb.genesis.ui.components.rememberTrackMutator()
                        val density = LocalDensity.current
                        val haptic = LocalHapticFeedback.current
                        val currentTracks = rememberUpdatedState(tracks)
                        val currentSelection = rememberUpdatedState(s.selectedIds)
                        val currentReorderMode = rememberUpdatedState(s.reorderMode)
                        val edgeZonePx = with(density) { 80.dp.toPx() }
                        val maxScrollPxPerFrame = with(density) { 14.dp.toPx() }
                        val accentWidthPx = with(density) { NOW_PLAYING_ACCENT_WIDTH.toPx() }
                        val handleEndPx = with(density) {
                            (NOW_PLAYING_ACCENT_WIDTH + DRAG_HANDLE_WIDTH).toPx()
                        }

                        // Drag-to-reorder state. Held in the screen (not the VM) because it's
                        // pure UI choreography — the VM only sees the final committed move.
                        var draggingId by remember { mutableStateOf<Long?>(null) }
                        var dragFromIndex by remember { mutableIntStateOf(-1) }
                        // Live preview of where the dragged row would land. Updated in the
                        // auto-scroll loop. Drives the per-row shift translations so the user
                        // sees rows part to make space for the dragged item.
                        var dragToIndex by remember { mutableIntStateOf(-1) }
                        var dragOffsetY by remember { mutableFloatStateOf(0f) }
                        // Captured at drag start. All TrackRows are uniform height so one
                        // value is enough to compute neighbor shifts.
                        var rowHeightPx by remember { mutableFloatStateOf(0f) }
                        val draggingState = rememberUpdatedState(draggingId)

                        // While a drag is active, sit in an auto-scroll loop: if the dragged row's
                        // visual center is in the top/bottom edge zone, scroll the list AND bump
                        // dragOffsetY by the same amount so the row stays pinned to the finger.
                        LaunchedEffect(draggingId) {
                            if (draggingId == null) return@LaunchedEffect
                            while (isActive) {
                                val from = dragFromIndex
                                if (from < 0) break
                                val info = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.index == from }
                                if (info != null) {
                                    val centerScrollY = info.offset + info.size / 2f + dragOffsetY
                                    val localY = centerScrollY - listState.layoutInfo.viewportStartOffset
                                    val viewportH = (
                                        listState.layoutInfo.viewportEndOffset -
                                            listState.layoutInfo.viewportStartOffset
                                        ).toFloat()
                                    val speed = when {
                                        localY < edgeZonePx ->
                                            -maxScrollPxPerFrame *
                                                ((edgeZonePx - localY) / edgeZonePx).coerceIn(0f, 1f)
                                        localY > viewportH - edgeZonePx ->
                                            maxScrollPxPerFrame *
                                                ((localY - (viewportH - edgeZonePx)) / edgeZonePx)
                                                    .coerceIn(0f, 1f)
                                        else -> 0f
                                    }
                                    if (speed != 0f) {
                                        val before = listState.firstVisibleItemScrollOffset +
                                            listState.firstVisibleItemIndex * 100_000
                                        listState.scrollBy(speed)
                                        // Compensate translation so the row stays under the finger
                                        // as the list slides under it.
                                        dragOffsetY += speed
                                        // Avoid pegged CPU if list refused to scroll (already at edge)
                                        val after = listState.firstVisibleItemScrollOffset +
                                            listState.firstVisibleItemIndex * 100_000
                                        if (before == after) {
                                            // List didn't move — clamp the bump back so we don't
                                            // accumulate phantom offset.
                                            dragOffsetY -= speed
                                        }
                                    }
                                }
                                // Refresh the preview destination so neighboring rows shift to
                                // open the slot the dragged row would land in.
                                val resolved = resolveDragTarget(listState, from, dragOffsetY)
                                if (resolved != null && resolved != dragToIndex) {
                                    dragToIndex = resolved
                                }
                                delay(16)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    // Long-press → drag-select. Touches on the drag handle
                                    // (between the leading accent strip and the TrackRow body)
                                    // are owned by that row's own pointerInput for drag-to-reorder,
                                    // so we bail. In selection mode the handle is hidden, so
                                    // long-press anywhere extends selection as usual.
                                    val viewportHeight = size.height.toFloat()
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        // In reorder mode, taps belong to the per-row handle's
                                        // own pointerInput — never extend selection from here.
                                        if (currentReorderMode.value) return@awaitEachGesture
                                        val isInSelectionMode = currentSelection.value.isNotEmpty()
                                        val onHandle = down.position.x >= accentWidthPx &&
                                            down.position.x < handleEndPx
                                        if (!isInSelectionMode && onHandle) return@awaitEachGesture
                                        val initialY = down.position.y
                                        awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                                        // Defensive: if a reorder gesture got there first
                                        // (shouldn't, since we bailed on x above), don't
                                        // double-toggle the same row.
                                        if (draggingState.value != null) return@awaitEachGesture

                                        val first = trackAtY(listState, currentTracks.value, initialY)
                                            ?: return@awaitEachGesture
                                        val wasSelected = first.mediaStoreId in currentSelection.value
                                        val target = !wasSelected
                                        viewModel.toggleSelection(first.mediaStoreId)
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
                                                val t = trackAtY(listState, currentTracks.value, y)
                                                if (t != null && t.mediaStoreId !in visited) {
                                                    visited += t.mediaStoreId
                                                    val isSel = t.mediaStoreId in currentSelection.value
                                                    if (isSel != target) viewModel.toggleSelection(t.mediaStoreId)
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
                            // Reserve space at the bottom so the last tracks can scroll above
                            // the small FAB instead of sitting under it.
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                bottom = 72.dp,
                            ),
                        ) {
                            // Footer pill — sits below the last track. When the user
                            // scrolls to it, the FAB hides (see [addButtonVisible] above)
                            // so there's exactly one add affordance on screen at a time.
                            // Hidden in selection/reorder mode to match the FAB behavior.
                            val footerVisible = !s.selectionMode && !s.reorderMode
                            items(
                                count = tracks.size,
                                key = { idx -> tracks[idx].mediaStoreId },
                            ) { idx ->
                                val track = tracks[idx]
                                val selected = track.mediaStoreId in s.selectedIds
                                val isCurrent = s.currentMediaStoreId != null &&
                                    track.mediaStoreId == s.currentMediaStoreId
                                val isDragged = draggingId == track.mediaStoreId
                                val currentIdx by rememberUpdatedState(idx)

                                // Live shift preview: while a drag is active, rows between the
                                // source and the live drop target slide one row's height to open
                                // the destination slot. The dragged row itself uses dragOffsetY
                                // directly (below). Animated so the shift glides instead of snapping.
                                val targetShift: Float = when {
                                    draggingId == null -> 0f
                                    isDragged -> 0f
                                    rowHeightPx == 0f -> 0f
                                    dragFromIndex == -1 || dragToIndex == -1 -> 0f
                                    dragFromIndex == dragToIndex -> 0f
                                    dragFromIndex < dragToIndex &&
                                        idx in (dragFromIndex + 1)..dragToIndex -> -rowHeightPx
                                    dragFromIndex > dragToIndex &&
                                        idx in dragToIndex..(dragFromIndex - 1) -> rowHeightPx
                                    else -> 0f
                                }
                                val animatedShift by animateFloatAsState(
                                    targetValue = targetShift,
                                    animationSpec = tween(durationMillis = SHIFT_ANIM_MS),
                                    label = "drag-shift",
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isDragged) {
                                                Modifier
                                                    .zIndex(1f)
                                                    .graphicsLayer { translationY = dragOffsetY }
                                            } else Modifier.graphicsLayer { translationY = animatedShift }
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Always-rendered (transparent when inactive) so the row's
                                    // horizontal layout is identical whether or not a track is
                                    // currently playing.
                                    Box(
                                        modifier = Modifier
                                            .width(NOW_PLAYING_ACCENT_WIDTH)
                                            .height(DRAG_HANDLE_HEIGHT)
                                            .background(
                                                if (isCurrent) MaterialTheme.colorScheme.primary
                                                else Color.Transparent,
                                            ),
                                    )
                                    // Drag handle slides left and collapses (shrinkHorizontally)
                                    // when selection mode kicks in, mirroring the checkbox's
                                    // entrance from the same edge inside TrackRow.
                                    AnimatedVisibility(
                                        visible = s.reorderMode,
                                        enter = slideInHorizontally(tween(SHIFT_ANIM_MS)) { -it } +
                                            expandHorizontally(tween(SHIFT_ANIM_MS)) +
                                            fadeIn(tween(SHIFT_ANIM_MS)),
                                        exit = slideOutHorizontally(tween(SHIFT_ANIM_MS)) { -it } +
                                            shrinkHorizontally(tween(SHIFT_ANIM_MS)) +
                                            fadeOut(tween(SHIFT_ANIM_MS)),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(DRAG_HANDLE_WIDTH)
                                                .height(DRAG_HANDLE_HEIGHT)
                                                .pointerInput(track.mediaStoreId) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            draggingId = track.mediaStoreId
                                                            dragFromIndex = currentIdx
                                                            dragToIndex = currentIdx
                                                            dragOffsetY = 0f
                                                            val info = listState.layoutInfo
                                                                .visibleItemsInfo
                                                                .firstOrNull { it.index == currentIdx }
                                                            rowHeightPx = info?.size?.toFloat() ?: 0f
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.LongPress,
                                                            )
                                                        },
                                                        onDrag = { change, drag ->
                                                            change.consume()
                                                            dragOffsetY += drag.y
                                                        },
                                                        onDragEnd = {
                                                            val from = dragFromIndex
                                                            val to = resolveDragTarget(
                                                                listState, from, dragOffsetY,
                                                            )
                                                            if (to != null && to != from) {
                                                                viewModel.moveTrack(from, to)
                                                            }
                                                            draggingId = null
                                                            dragFromIndex = -1
                                                            dragToIndex = -1
                                                            dragOffsetY = 0f
                                                            rowHeightPx = 0f
                                                        },
                                                        onDragCancel = {
                                                            draggingId = null
                                                            dragFromIndex = -1
                                                            dragToIndex = -1
                                                            dragOffsetY = 0f
                                                            rowHeightPx = 0f
                                                        },
                                                    )
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DragHandle,
                                                contentDescription = dragHandleLabel,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.alpha(0.7f),
                                            )
                                        }
                                    }
                                    TrackRow(
                                        modifier = Modifier.weight(1f),
                                        track = track,
                                        selected = selected,
                                        selectionMode = s.selectionMode,
                                        isCurrentlyPlaying = isCurrent,
                                        // Accent rendered above by the wrapping Row instead.
                                        showNowPlayingAccent = false,
                                        actions = if (!s.selectionMode) {
                                            listOf(
                                                TrackAction(
                                                    icon = Icons.Filled.Delete,
                                                    label = removeLabel,
                                                    onClick = { viewModel.removeOne(track.mediaStoreId) },
                                                ),
                                                mutator.renameAction(track),
                                                mutator.deleteAction(track),
                                            )
                                        } else emptyList(),
                                        onClick = {
                                            if (s.selectionMode) viewModel.toggleSelection(track.mediaStoreId)
                                            else viewModel.play(idx)
                                        },
                                        // Long-press handled by the parent's pointerInput so the SAME gesture
                                        // can extend into drag-select. Forwarding here would double-toggle.
                                        onLongClick = {},
                                    )
                                }
                            }
                            if (footerVisible) {
                                item(key = "add-tracks-footer") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = Spacing.lg),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        FilledTonalButton(
                                            onClick = { onAddTracks(viewModel.playlistId) },
                                            shape = RoundedCornerShape(50),
                                            contentPadding = androidx.compose.foundation.layout
                                                .PaddingValues(
                                                    horizontal = Spacing.lg,
                                                    vertical = Spacing.sm,
                                                ),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.add_tracks),
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val NOW_PLAYING_ACCENT_WIDTH = 3.dp
private val DRAG_HANDLE_WIDTH = 40.dp
private val DRAG_HANDLE_HEIGHT = 56.dp
private const val SHIFT_ANIM_MS = 200

private fun trackAtY(state: LazyListState, tracks: List<Track>, y: Float): Track? {
    // Same coordinate conversion as the library list: pointer Y is in LazyColumn-local
    // coords, LazyListItemInfo.offset is in scroll-content coords; they differ by
    // -viewportStartOffset (= beforeContentPadding).
    val scrollY = y + state.layoutInfo.viewportStartOffset
    val info = state.layoutInfo.visibleItemsInfo.firstOrNull {
        scrollY >= it.offset && scrollY < it.offset + it.size
    } ?: return null
    return tracks.getOrNull(info.index)
}

/**
 * Resolve the destination index for a drag-reorder gesture by finding which row's
 * vertical bounds contain the dragged row's current visual center (in scroll-content
 * coords). Returns the source index if center hasn't crossed a neighbor — caller
 * should compare and skip a no-op move.
 */
private fun resolveDragTarget(
    state: LazyListState,
    fromIndex: Int,
    dragOffsetY: Float,
): Int? {
    val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == fromIndex }
        ?: return null
    val center = info.offset + info.size / 2f + dragOffsetY
    val target = state.layoutInfo.visibleItemsInfo.firstOrNull {
        center >= it.offset && center < it.offset + it.size
    } ?: return null
    return target.index
}

@Composable
private fun CompactHeaderRow(
    navIcon: @Composable () -> Unit,
    title: String,
    actions: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        navIcon()
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.sm),
        )
        actions?.invoke()
    }
}

@Composable
private fun SelectionActionBar(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
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
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = stringResource(R.string.move_up))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
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

