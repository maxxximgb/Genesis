package dev.maxxximgb.genesis.ui.nowPlaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.data.media.AlbumArt
import dev.maxxximgb.genesis.domain.model.LoopState
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.model.loopStateOf
import dev.maxxximgb.genesis.ui.components.AlbumArtImage
import dev.maxxximgb.genesis.ui.theme.Corner
import dev.maxxximgb.genesis.ui.theme.Sizes
import dev.maxxximgb.genesis.ui.theme.Spacing
import dev.maxxximgb.genesis.ui.util.formatDuration

private const val EXPAND_ANIM_MS = 200
private val DISMISS_THRESHOLD = 150.dp
private const val DISMISS_VELOCITY_PX_PER_S = 1200f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullNowPlayingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val queue by viewModel.queueTracks.collectAsStateWithLifecycle()
    val sleepRemaining by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    val livePosition by viewModel.livePositionMs.collectAsStateWithLifecycle()
    val eqMasterEnabled by viewModel.eqMasterEnabled.collectAsStateWithLifecycle()
    val eqSupported by viewModel.eqSupported.collectAsStateWithLifecycle()

    var showSleepSheet by remember { mutableStateOf(false) }
    var showEnqueueSheet by remember { mutableStateOf(false) }
    var queueExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val addedToQueueFmt = stringResource(R.string.snack_added_to_queue)

    val currentTrack = queue.getOrNull(state.currentIndex)
        ?: queue.firstOrNull { it.mediaStoreId == state.currentMediaStoreId }

    // Swipe-down to dismiss. Disabled when queue is expanded (so the queue list inside still
    // owns its scroll/click gestures).
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { DISMISS_THRESHOLD.toPx() }
    val dragScope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }
    val dragState = rememberDraggableState { delta ->
        dragScope.launch {
            dragOffsetY.snapTo((dragOffsetY.value + delta).coerceAtLeast(0f))
        }
    }
    val dismissModifier = if (queueExpanded) {
        Modifier
    } else {
        Modifier.draggable(
            state = dragState,
            orientation = Orientation.Vertical,
            onDragStopped = { velocity ->
                if (dragOffsetY.value > dismissThresholdPx || velocity > DISMISS_VELOCITY_PX_PER_S) {
                    onNavigateBack()
                } else {
                    dragOffsetY.animateTo(0f, animationSpec = tween(EXPAND_ANIM_MS))
                }
            },
        )
    }

    // playOrderIndices walks the player's shuffle-aware play order — element 0 is the
    // currently playing track, the rest is the upcoming queue in actual play order. We drop
    // element 0 (current is shown in the hero section, not the queue) and look up tracks by
    // player-MediaItem-index. Falls back to natural order if playOrderIndices is empty
    // (player not yet ready).
    val upcoming = remember(queue, state.currentIndex, state.playOrderIndices) {
        if (state.playOrderIndices.isNotEmpty()) {
            state.playOrderIndices.drop(1).mapNotNull { queue.getOrNull(it) }
        } else {
            val from = (state.currentIndex + 1).coerceAtLeast(0)
            if (from >= queue.size) emptyList() else queue.drop(from)
        }
    }
    val upcomingPlayerIndices = remember(state.currentIndex, state.playOrderIndices, queue.size) {
        if (state.playOrderIndices.isNotEmpty()) {
            state.playOrderIndices.drop(1)
        } else {
            val from = (state.currentIndex + 1).coerceAtLeast(0)
            (from until queue.size).toList()
        }
    }


    Scaffold(
        modifier = dismissModifier.graphicsLayer { translationY = dragOffsetY.value },
        // No topBar: the M3 TopAppBar adds ~64dp on top of the status-bar inset, which
        // shows up as dead vertical space above the artwork. Instead, the back-arrow is
        // an inline IconButton at the start of the content Column, so the only top
        // padding is the system status-bar inset itself.
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val parentHeightDp = maxHeight
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar: back-arrow on the left, sleep-timer IconButton on the right.
                // No M3 TopAppBar — see comment on Scaffold above for why we keep this inline.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { showSleepSheet = true }) {
                        Icon(
                            Icons.Filled.Bedtime,
                            contentDescription = stringResource(R.string.sleep_timer_title),
                            tint = if (sleepRemaining != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
                HeroSection(
                    track = currentTrack,
                    positionMs = livePosition,
                    durationMs = state.durationMs,
                    isPlaying = state.isPlaying,
                    loopState = loopStateOf(state.repeatMode, state.shuffleEnabled),
                    queueSize = state.queue.size,
                    eqEnabled = eqMasterEnabled,
                    eqSupported = eqSupported,
                    onSeekTo = viewModel::seekTo,
                    onPrevious = viewModel::previous,
                    onNext = viewModel::next,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onCycleLoop = viewModel::cycleLoop,
                    onLongPressLoop = viewModel::reshuffleIfShuffleActive,
                    onOpenEqualizer = onNavigateToEqualizer,
                )
            }
            // Queue panel: collapsed = just the 20dp grabber pill; expanded = full takeover.
            // Visible whenever something is loaded into the player so the user can always
            // reach the "Add to queue" button — even on the last track of a playlist where
            // `upcoming` is empty. The grabber alone is 20dp, so the cost of always-showing
            // is minimal vs. the previous behavior of hiding the panel entirely.
            AnimatedVisibility(
                visible = state.currentMediaStoreId != null || queueExpanded,
                enter = slideInVertically(
                    animationSpec = tween(EXPAND_ANIM_MS),
                    initialOffsetY = { it },
                ),
                exit = slideOutVertically(
                    animationSpec = tween(EXPAND_ANIM_MS),
                    targetOffsetY = { it },
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                UpNextPanel(
                    upcoming = upcoming,
                    upcomingPlayerIndices = upcomingPlayerIndices,
                    onJump = viewModel::jumpToQueueIndex,
                    onMove = viewModel::moveQueueItem,
                    onExpandedChange = { queueExpanded = it },
                    onAddToQueue = { showEnqueueSheet = true },
                    parentHeightDp = parentHeightDp,
                )
            }
        }
    }

    if (showSleepSheet) {
        SleepTimerSheet(
            currentRemainingMs = sleepRemaining,
            onDismiss = { showSleepSheet = false },
            onPick = { minutes ->
                viewModel.setSleepTimerMinutes(minutes)
                showSleepSheet = false
            },
            onCancel = {
                viewModel.cancelSleepTimer()
                showSleepSheet = false
            },
        )
    }

    if (showEnqueueSheet) {
        // All three flows are collected only while this sheet is visible — `WhileSubscribed(0)`
        // on the VM side stops the upstream Room queries the moment the sheet dismisses,
        // so the now-playing screen doesn't pay the album/playlist/track read cost otherwise.
        val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
        val albums by viewModel.allAlbums.collectAsStateWithLifecycle()
        val tracks by viewModel.allTracks.collectAsStateWithLifecycle()
        val enqueueScope = rememberCoroutineScope()
        EnqueueSheet(
            playlists = playlists,
            albums = albums,
            tracks = tracks,
            onEnqueuePlaylist = { id ->
                enqueueScope.launch {
                    val n = viewModel.enqueuePlaylistById(id)
                    showEnqueueSheet = false
                    if (n > 0) snackbarHostState.showSnackbar(addedToQueueFmt.format(n))
                }
            },
            onEnqueueAlbum = { id ->
                enqueueScope.launch {
                    val n = viewModel.enqueueAlbumById(id)
                    showEnqueueSheet = false
                    if (n > 0) snackbarHostState.showSnackbar(addedToQueueFmt.format(n))
                }
            },
            onEnqueueSelectedTracks = { ids ->
                enqueueScope.launch {
                    val n = viewModel.enqueueSelectedTrackIds(ids)
                    showEnqueueSheet = false
                    if (n > 0) snackbarHostState.showSnackbar(addedToQueueFmt.format(n))
                }
            },
            onDismiss = { showEnqueueSheet = false },
        )
    }
}

// Grabber pill area only: top sm (8) + pill 4 + bottom xs (4) = 16dp. Plus a 4dp tail
// so the pill has a hair of breathing room above the system gesture bar. No track-row
// peek by default — user explicitly asked the queue not to "stick out" when idle.
private val QUEUE_COLLAPSED_HEIGHT = 20.dp
// Full takeover: the queue can be dragged to cover the entire content area below the
// status bar. The grabber pill at top doubles as the close affordance (swipe down or tap).
private const val QUEUE_EXPANDED_FRACTION = 1.0f
private const val FLING_VELOCITY_THRESHOLD = 1000f

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UpNextPanel(
    upcoming: List<Track>,
    upcomingPlayerIndices: List<Int>,
    onJump: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onAddToQueue: () -> Unit,
    parentHeightDp: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    var reorderMode by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val collapsedPx = with(density) { QUEUE_COLLAPSED_HEIGHT.toPx() }
    val expandedPx = with(density) { (parentHeightDp * QUEUE_EXPANDED_FRACTION).toPx() }
        .coerceAtLeast(collapsedPx + 100f)

    val scope = rememberCoroutineScope()
    val panelHeight = remember { Animatable(collapsedPx) }
    var expanded by remember { mutableStateOf(false) }
    // Captured at drag start so we can decide on release whether the user pulled the panel
    // open (any growth → expand) or pushed it closed (any shrink → collapse), instead of
    // snapping by mid-position which "opened only a little" on small drags.
    var dragStartHeight by remember { mutableStateOf(collapsedPx) }

    fun setExpanded(value: Boolean) {
        scope.launch {
            panelHeight.animateTo(
                if (value) expandedPx else collapsedPx,
                animationSpec = tween(EXPAND_ANIM_MS),
            )
        }
        if (expanded != value) {
            expanded = value
            onExpandedChange(value)
        }
    }

    // While dragging the handle, panel height tracks the finger directly. Compose's vertical
    // delta convention: positive = down, so subtracting from height grows the panel when the
    // user drags up.
    val dragState = rememberDraggableState { delta ->
        scope.launch {
            val target = (panelHeight.value - delta).coerceIn(collapsedPx, expandedPx)
            panelHeight.snapTo(target)
        }
    }

    val titleVisible by remember {
        derivedStateOf { panelHeight.value > collapsedPx + 80f }
    }
    // "Peek" = panel is at (or very near) collapsed. In that state the LazyColumn must NOT
    // scroll — the user expects taps and upward swipes anywhere on the visible row to expand
    // the panel instead of paging through tracks they can't see anyway.
    val isPeek by remember {
        derivedStateOf { panelHeight.value < collapsedPx + 30f }
    }

    // Hoisted so the nested-scroll bridge below can gate "swipe-down closes panel" on
    // the list actually being scrolled to its top. Without this check, a fast downward
    // fling from the middle of the list (intent: scroll up through items) would also
    // close the panel — user explicitly asked for the close to fire only at the top.
    val listState = rememberLazyListState()
    val listAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
        }
    }

    // NestedScroll bridge: when the LazyColumn is at scroll-top AND the user keeps
    // dragging downward, the leftover delta shrinks the panel toward collapsed. From
    // any other scroll position the list owns the gesture and the panel stays put.
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val delta = available.y
                if (delta > 0f && listAtTop && panelHeight.value > collapsedPx) {
                    val newHeight = (panelHeight.value - delta).coerceAtLeast(collapsedPx)
                    val absorbed = panelHeight.value - newHeight
                    scope.launch { panelHeight.snapTo(newHeight) }
                    return Offset(0f, absorbed)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Only close on a downward fling when the list is already at the top —
                // otherwise a fast swipe-up-through-items would unintentionally collapse
                // the panel.
                if (available.y > FLING_VELOCITY_THRESHOLD &&
                    listAtTop &&
                    panelHeight.value > collapsedPx
                ) {
                    setExpanded(false)
                    return Velocity(0f, available.y)
                }
                return Velocity.Zero
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(with(density) { panelHeight.value.toDp() }),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Grabber: the only place that captures vertical drag for resizing the panel
            // (the LazyColumn below owns its own scroll). Tap also toggles.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStarted = { dragStartHeight = panelHeight.value },
                        onDragStopped = { velocity ->
                            val targetExpanded = when {
                                velocity < -FLING_VELOCITY_THRESHOLD -> true
                                velocity > FLING_VELOCITY_THRESHOLD -> false
                                // Any growth opens; any shrink closes.
                                else -> panelHeight.value > dragStartHeight
                            }
                            setExpanded(targetExpanded)
                        },
                    )
                    .clickable(onClick = { setExpanded(!expanded) })
                    .padding(top = Spacing.sm, bottom = Spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        ),
                )
            }

            // Title row appears once the panel is taller than the peek state. Shows a
            // SwapVert toggle on the right that activates "reorder mode" — drag handles
            // come in only then, matching the playlist-detail pattern (user explicitly
            // didn't want handles eating row width by default).
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(150)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Spacing.lg,
                            end = Spacing.xs,
                            top = Spacing.sm,
                            bottom = Spacing.xs,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (reorderMode) {
                            stringResource(R.string.reorder_tracks)
                        } else {
                            stringResource(R.string.queue_title)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (upcoming.isNotEmpty()) {
                        IconButton(onClick = { reorderMode = !reorderMode }) {
                            Icon(
                                imageVector = if (reorderMode) {
                                    Icons.Filled.Close
                                } else {
                                    Icons.Filled.SwapVert
                                },
                                contentDescription = stringResource(R.string.reorder_tracks),
                                tint = if (reorderMode) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }

            if (upcoming.isEmpty()) {
                // Only render the empty placeholder once the panel is past peek — otherwise
                // a redundant "Queue" label peeks under the grabber, duplicating the panel's
                // own title and making the panel "stick out" when there's nothing to show.
                if (titleVisible) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(Spacing.lg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.queue_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                // The pointerInput stays attached for the lifetime of the panel — making it
                // conditional on `isPeek` would tear down the awaitEachGesture coroutine the
                // moment the panel grows past the peek threshold, cancelling the drag mid-way
                // and leaving the queue stuck near the collapsed state. Instead, we decide
                // once at gesture start whether this is a peek-drag and own the entire gesture
                // until release.
                val peekGestureModifier = Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startedAtPeek = panelHeight.value < collapsedPx + 30f
                        if (!startedAtPeek) {
                            // Panel is already expanded — let the LazyColumn scroll handle it.
                            return@awaitEachGesture
                        }
                        val change = awaitTouchSlopOrCancellation(down.id) { c, dragAmount ->
                            scope.launch {
                                val target = (panelHeight.value - dragAmount.y)
                                    .coerceIn(collapsedPx, expandedPx)
                                panelHeight.snapTo(target)
                            }
                            c.consume()
                        }
                        if (change == null) {
                            // Released without crossing slop — tap. Per the user's
                            // ask, taps in peek do nothing (only drag opens the queue).
                            return@awaitEachGesture
                        }
                        // Drag has begun. Stay glued to the finger until release.
                        verticalDrag(change.id) { drag ->
                            val delta = drag.positionChange().y
                            scope.launch {
                                val target = (panelHeight.value - delta)
                                    .coerceIn(collapsedPx, expandedPx)
                                panelHeight.snapTo(target)
                            }
                            drag.consume()
                        }
                        // Any amount of upward drag (panelHeight grew at all) commits
                        // to a full expand on release — the user explicitly asked for
                        // "pulled a tiny bit up → fully open" instead of nearest-anchor.
                        if (panelHeight.value > collapsedPx) {
                            setExpanded(true)
                        }
                    }
                }
                // Drag-to-reorder state. Same model as PlaylistDetailScreen: indices are
                // into the visible `upcoming` list, NOT the player timeline — we map back
                // to timeline indices via `upcomingPlayerIndices` only when committing on
                // release. Live shift preview: while dragging, neighbour rows animate a
                // one-row translation toward the dragged-from slot, opening the destination.
                var draggingListIndex by remember { mutableStateOf<Int?>(null) }
                var dragFromIndex by remember { mutableIntStateOf(-1) }
                var dragToIndex by remember { mutableIntStateOf(-1) }
                var dragOffsetY by remember { mutableFloatStateOf(0f) }
                var rowHeightPx by remember { mutableFloatStateOf(0f) }
                val edgeZonePx = with(density) { 80.dp.toPx() }
                val maxScrollPxPerFrame = with(density) { 14.dp.toPx() }

                // Auto-scroll loop: if the dragged row's visual center is in the top/bottom
                // edge zone of the viewport, scroll the list AND bump dragOffsetY by the
                // same amount so the row stays pinned to the finger. Also continuously
                // refreshes dragToIndex so the shift preview tracks the live drop target.
                LaunchedEffect(draggingListIndex) {
                    if (draggingListIndex == null) return@LaunchedEffect
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
                                dragOffsetY += speed
                                val after = listState.firstVisibleItemScrollOffset +
                                    listState.firstVisibleItemIndex * 100_000
                                // Clamp back if the list refused to move (already at edge),
                                // otherwise we'd accumulate phantom offset that the row
                                // can't visually catch up to.
                                if (before == after) dragOffsetY -= speed
                            }
                        }
                        val resolved = resolveQueueDragTarget(listState, from, dragOffsetY)
                        if (resolved != null && resolved != dragToIndex) {
                            dragToIndex = resolved
                        }
                        delay(16)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(nestedScrollConnection)
                        .then(peekGestureModifier),
                    userScrollEnabled = !isPeek,
                ) {
                    items(
                        count = upcoming.size,
                        // Key on player timeline index — stable across recompositions even
                        // with duplicates; mediaStoreId alone would collide if the same
                        // track appears twice in the queue.
                        key = { idx -> "t:${upcomingPlayerIndices.getOrNull(idx) ?: idx}" },
                    ) { idx ->
                        val track = upcoming[idx]
                        val isDragged = draggingListIndex == idx

                        // Live shift preview: rows between source and live drop target
                        // slide one row's height toward the source, opening the destination.
                        // Animated so the shift glides instead of snapping each time
                        // dragToIndex flips to a new neighbour.
                        val targetShift: Float = when {
                            draggingListIndex == null -> 0f
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
                            animationSpec = tween(durationMillis = 200),
                            label = "queue-drag-shift",
                        )

                        Box(
                            modifier = if (isDragged) {
                                Modifier
                                    .zIndex(1f)
                                    .graphicsLayer { translationY = dragOffsetY }
                            } else {
                                Modifier.graphicsLayer { translationY = animatedShift }
                            },
                        ) {
                            QueueItem(
                                track = track,
                                interactive = !isPeek,
                                onClick = { onJump(upcomingPlayerIndices[idx]) },
                                showDragHandle = reorderMode,
                                onDragStart = {
                                    draggingListIndex = idx
                                    dragFromIndex = idx
                                    dragToIndex = idx
                                    dragOffsetY = 0f
                                    val info = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.index == idx }
                                    rowHeightPx = info?.size?.toFloat() ?: 0f
                                },
                                onDrag = { delta -> dragOffsetY += delta },
                                onDragEnd = {
                                    val fromList = dragFromIndex
                                    val toList = dragToIndex
                                    if (fromList >= 0 && toList >= 0 && fromList != toList) {
                                        val fromTl = upcomingPlayerIndices.getOrNull(fromList)
                                        val toTl = upcomingPlayerIndices.getOrNull(toList)
                                        if (fromTl != null && toTl != null) onMove(fromTl, toTl)
                                    }
                                    draggingListIndex = null
                                    dragFromIndex = -1
                                    dragToIndex = -1
                                    dragOffsetY = 0f
                                    rowHeightPx = 0f
                                },
                                onDragCancel = {
                                    draggingListIndex = null
                                    dragFromIndex = -1
                                    dragToIndex = -1
                                    dragOffsetY = 0f
                                    rowHeightPx = 0f
                                },
                            )
                        }
                    }
                }
            }

            // "Add to queue" pill anchored at the bottom of the panel — always visible
            // when expanded, regardless of whether `upcoming` is empty (so the user can
            // start building a queue from a single-track or last-track state too).
            // Hidden in peek so it doesn't bleed into the grabber-only collapsed strip.
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(150)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    FilledTonalButton(
                        onClick = onAddToQueue,
                        shape = RoundedCornerShape(50),
                        contentPadding = androidx.compose.foundation.layout
                            .PaddingValues(
                                horizontal = Spacing.lg,
                                vertical = Spacing.sm,
                            ),
                    ) {
                        Text(
                            text = stringResource(R.string.add_to_queue),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSection(
    track: Track?,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    loopState: LoopState,
    queueSize: Int,
    eqEnabled: Boolean,
    eqSupported: Boolean,
    onSeekTo: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onCycleLoop: () -> Unit,
    onLongPressLoop: () -> Unit,
    onOpenEqualizer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeroArtwork(albumId = track?.albumId)
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = track?.title ?: "",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = track?.artist ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.lg))

        SeekBar(
            positionMs = positionMs,
            durationMs = durationMs,
            onSeekTo = onSeekTo,
            trackKey = track?.mediaStoreId,
        )
        Spacer(Modifier.height(Spacing.md))
        TransportRow(
            isPlaying = isPlaying,
            loopState = loopState,
            queueSize = queueSize,
            eqEnabled = eqEnabled,
            eqSupported = eqSupported,
            onPrevious = onPrevious,
            onNext = onNext,
            onTogglePlayPause = onTogglePlayPause,
            onCycleLoop = onCycleLoop,
            onLongPressLoop = onLongPressLoop,
            onOpenEqualizer = onOpenEqualizer,
        )
    }
}

@Composable
private fun HeroArtwork(albumId: Long?) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(Corner.lg)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        // Placeholder icon lives in the outer Box's centered slot so its fillMaxSize(fraction)
        // actually constrains it. Putting it inside SubcomposeAsyncImage's content slot —
        // as the previous version did — would force fillMaxSize on the slot and stretch the
        // music note to the full hero, ignoring the size modifier.
        Box(
            modifier = Modifier.fillMaxSize(0.28f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (albumId != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(AlbumArt.uri(albumId))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (painter.state is AsyncImagePainter.State.Success) {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    }
}

@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    trackKey: Long?,
) {
    // Two-state model:
    //  - draggingValue: live finger position while the user is touching the slider.
    //  - pendingSeek:   the released-but-not-yet-confirmed seek target. Held until the
    //                   incoming positionMs catches up, otherwise the thumb visibly snaps
    //                   back to the stale state.positionMs before the player propagates.
    var draggingValue by remember(trackKey) { mutableStateOf<Float?>(null) }
    var pendingSeek by remember(trackKey) { mutableStateOf<Float?>(null) }
    val max = durationMs.toFloat().coerceAtLeast(1f)
    val displayed = (draggingValue ?: pendingSeek ?: positionMs.toFloat()).coerceIn(0f, max)

    LaunchedEffect(positionMs, pendingSeek) {
        val pending = pendingSeek ?: return@LaunchedEffect
        if (kotlin.math.abs(positionMs - pending.toLong()) < SEEK_SETTLE_TOLERANCE_MS) {
            pendingSeek = null
        }
    }

    Slider(
        value = displayed,
        onValueChange = { draggingValue = it },
        onValueChangeFinished = {
            val v = draggingValue ?: return@Slider
            pendingSeek = v
            draggingValue = null
            onSeekTo(v.toLong())
        },
        valueRange = 0f..max,
        enabled = durationMs > 0L,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatDuration(displayed.toLong()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatDuration(durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val SEEK_SETTLE_TOLERANCE_MS = 1500L

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransportRow(
    isPlaying: Boolean,
    loopState: LoopState,
    queueSize: Int,
    eqEnabled: Boolean,
    eqSupported: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onCycleLoop: () -> Unit,
    onLongPressLoop: () -> Unit,
    onOpenEqualizer: () -> Unit,
) {
    val canSkip = queueSize > 1
    // 5 equal slots: [eq] | prev | play | next | loop. Equalizer entry sits on the left to
    // balance the loop button on the right so play stays exactly in the middle. The sleep
    // timer moved to the top-right corner of the screen.
    //
    // When the device doesn't support EQ the leftmost slot is left empty (not removed) — the
    // 5-slot symmetry is part of the Now Playing visual contract; collapsing to 4 slots would
    // shift play off-center.
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportSlot {
            if (eqSupported) {
                IconButton(onClick = onOpenEqualizer) {
                    // Active state: small primary-coloured dot below the icon. Subtle
                    // affordance — the icon shape stays the same, the dot is just enough to
                    // signal on/off without competing visually with the other transport icons.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            Icons.Filled.GraphicEq,
                            contentDescription = stringResource(R.string.equalizer_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (eqEnabled) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                    ),
                            )
                        } else {
                            Spacer(modifier = Modifier.size(4.dp))
                        }
                    }
                }
            }
        }
        TransportSlot {
            IconButton(onClick = onPrevious, enabled = canSkip) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.previous),
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        TransportSlot {
            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                    modifier = Modifier.size(48.dp),
                )
            }
        }
        TransportSlot {
            IconButton(onClick = onNext, enabled = canSkip) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.next),
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        TransportSlot {
            // Long-press while shuffle is active = re-randomize the shuffle order. The tap
            // still cycles loop state, the long-press is an extra affordance.
            val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.material.ripple.rememberRipple(bounded = false, radius = 20.dp),
                        onClick = onCycleLoop,
                        onLongClick = onLongPressLoop,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val (icon, cd) = when (loopState) {
                    LoopState.OFF -> Icons.Filled.Repeat to R.string.cd_loop_off
                    LoopState.REPEAT_ALL -> Icons.Filled.Repeat to R.string.cd_loop_all
                    LoopState.REPEAT_ONE -> Icons.Filled.RepeatOne to R.string.cd_loop_one
                    LoopState.SHUFFLE -> Icons.Filled.Shuffle to R.string.cd_shuffle
                }
                Icon(
                    icon,
                    contentDescription = stringResource(cd),
                    tint = if (loopState == LoopState.OFF) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}

@Composable
private fun RowScope.TransportSlot(content: @Composable () -> Unit) {
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueItem(
    track: Track,
    onClick: () -> Unit,
    interactive: Boolean = true,
    onLongClick: () -> Unit = {},
    /**
     * When true, a leading drag handle is rendered. Long-press the handle to start a
     * reorder; while held, [onDrag] receives vertical deltas and [onDragEnd] fires on
     * release. Tap-to-jump on the rest of the row continues to work — the gesture is
     * scoped to the handle's pointerInput.
     */
    showDragHandle: Boolean = false,
    onDragStart: () -> Unit = {},
    onDrag: (delta: Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val clickModifier = if (interactive) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        // No clickable in peek mode so the parent's pointerInput owns the gesture stream.
        // The whole row is presented as static content the user can grab and drag.
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(clickModifier)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (showDragHandle) {
            val haptic = LocalHapticFeedback.current
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 40.dp)
                    .pointerInput(track.mediaStoreId) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDragStart()
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                onDrag(drag.y)
                            },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = stringResource(R.string.cd_drag_to_reorder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AlbumArtImage(
            albumId = track.albumId,
            contentDescription = null,
            size = Sizes.albumArtMedium,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val artist = track.artist?.takeIf { it.isNotBlank() }
            if (artist != null) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Resolve the destination list-index for the queue drag-reorder gesture: find the
 * visible row whose vertical bounds contain the dragged row's current visual center
 * (in scroll-content coords). Returns the source index if the center hasn't crossed
 * a neighbour — caller should compare and skip a no-op move.
 */
private fun resolveQueueDragTarget(
    listState: LazyListState,
    fromIndex: Int,
    dragOffsetY: Float,
): Int? {
    val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == fromIndex }
        ?: return null
    val center = info.offset + info.size / 2f + dragOffsetY
    val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
        center >= it.offset && center < it.offset + it.size
    } ?: return null
    return target.index
}
