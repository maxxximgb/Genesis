package dev.maxxximgb.genesis.ui.nowPlaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.Album
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class EnqueueStage { SOURCE, ALBUMS, PLAYLISTS, TRACKS }

/**
 * Single sheet that walks the user through "what do you want to add to the queue?"
 *
 * Stage 1 (SOURCE) — three rows: Tracks / Albums / Playlists. Each transitions the
 * sheet to a picker stage in-place rather than opening nested ModalBottomSheets, which
 * on M3 stack awkwardly and lose the back-press affordance.
 *
 * Tracks picker supports drag-to-select: long-press a row to start selecting, then
 * keep finger down and drag — every row the finger crosses gets toggled to the same
 * state as the first one. Same gesture model as PlaylistDetailScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnqueueSheet(
    playlists: List<Playlist>,
    albums: List<Album>,
    tracks: List<Track>,
    onEnqueuePlaylist: (Long) -> Unit,
    onEnqueueAlbum: (Long) -> Unit,
    onEnqueueSelectedTracks: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var stage by remember { mutableStateOf(EnqueueStage.SOURCE) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        when (stage) {
            EnqueueStage.SOURCE -> SourcePicker(
                onTracks = { stage = EnqueueStage.TRACKS },
                onAlbums = { stage = EnqueueStage.ALBUMS },
                onPlaylists = { stage = EnqueueStage.PLAYLISTS },
            )
            EnqueueStage.ALBUMS -> AlbumPicker(
                albums = albums,
                onPick = { onEnqueueAlbum(it.id) },
                onBack = { stage = EnqueueStage.SOURCE },
            )
            EnqueueStage.PLAYLISTS -> PlaylistPicker(
                playlists = playlists,
                onPick = { onEnqueuePlaylist(it.id) },
                onBack = { stage = EnqueueStage.SOURCE },
            )
            EnqueueStage.TRACKS -> TrackPicker(
                tracks = tracks,
                onConfirm = onEnqueueSelectedTracks,
                onBack = { stage = EnqueueStage.SOURCE },
            )
        }
        Spacer(modifier = Modifier.height(Spacing.md))
    }
}

@Composable
private fun SourcePicker(
    onTracks: () -> Unit,
    onAlbums: () -> Unit,
    onPlaylists: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.add_to_queue),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.sm,
                bottom = Spacing.xs,
            ),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.browse_tracks)) },
            leadingContent = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onTracks),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.browse_albums)) },
            leadingContent = { Icon(Icons.Filled.Album, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onAlbums),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.browse_playlists)) },
            leadingContent = {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onPlaylists),
        )
    }
}

@Composable
private fun PlaylistPicker(
    playlists: List<Playlist>,
    onPick: (Playlist) -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(playlists, query) { filterPlaylists(playlists, query) }
    PickerScaffold(
        title = stringResource(R.string.browse_playlists),
        onBack = onBack,
        query = query,
        onQueryChange = { query = it },
        isEmpty = filtered.isEmpty(),
        emptyText = if (query.isBlank()) {
            stringResource(R.string.empty_playlists_title)
        } else {
            stringResource(R.string.search_no_results)
        },
    ) {
        items(items = filtered, key = { it.id }) { playlist ->
            ListItem(
                headlineContent = {
                    Text(
                        text = playlist.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
                },
                modifier = Modifier.clickable { onPick(playlist) },
            )
        }
    }
}

@Composable
private fun AlbumPicker(
    albums: List<Album>,
    onPick: (Album) -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(albums, query) { filterAlbums(albums, query) }
    PickerScaffold(
        title = stringResource(R.string.browse_albums),
        onBack = onBack,
        query = query,
        onQueryChange = { query = it },
        isEmpty = filtered.isEmpty(),
        emptyText = if (query.isBlank()) {
            stringResource(R.string.queue_empty)
        } else {
            stringResource(R.string.search_no_results)
        },
    ) {
        items(items = filtered, key = { it.id }) { album ->
            ListItem(
                headlineContent = {
                    Text(
                        text = album.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    val artist = album.artist?.takeIf { it.isNotBlank() }
                    if (artist != null) {
                        Text(
                            text = artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                leadingContent = { Icon(Icons.Filled.Album, contentDescription = null) },
                modifier = Modifier.clickable { onPick(album) },
            )
        }
    }
}

@Composable
private fun TrackPicker(
    tracks: List<Track>,
    onConfirm: (Set<Long>) -> Unit,
    onBack: () -> Unit,
) {
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var query by remember { mutableStateOf("") }
    val filteredTracks = remember(tracks, query) { filterTracks(tracks, query) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Captured by the gesture coroutine so it sees the freshest filtered list /
    // selection on every frame without being torn down on each recomposition.
    // Drag-to-select operates on the FILTERED list — the gesture only crosses rows
    // the user can actually see, which is what they expect.
    val currentTracks = rememberUpdatedState(filteredTracks)
    val currentSelection = rememberUpdatedState(selectedIds)
    val edgeZonePx = with(density) { 60.dp.toPx() }
    val maxScrollPxPerFrame = with(density) { 12.dp.toPx() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            Text(
                text = if (selectedIds.isEmpty()) {
                    stringResource(R.string.browse_tracks)
                } else {
                    stringResource(R.string.selected_count, selectedIds.size)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = Spacing.sm),
            )
        }
        PickerSearchBar(
            query = query,
            onQueryChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        )
        HorizontalDivider()

        if (filteredTracks.isEmpty()) {
            Text(
                text = if (query.isBlank()) {
                    stringResource(R.string.queue_empty)
                } else {
                    stringResource(R.string.search_no_results)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
            )
        } else {
            // Bigger than the playlist/album picker (420dp) — tracks lists run long and
            // a small viewport defeats the purpose of having a multi-select picker.
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .height(520.dp)
                    .pointerInput(Unit) {
                        // Drag-to-select gesture, ported from PlaylistDetailScreen:
                        // long-press a row to "anchor" the toggle direction, then keep
                        // dragging to extend selection across rows the finger crosses.
                        val viewportHeight = size.height.toFloat()
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val initialY = down.position.y
                            awaitLongPressOrCancellation(down.id)
                                ?: return@awaitEachGesture

                            val first = trackAtY(
                                listState, currentTracks.value, initialY,
                            ) ?: return@awaitEachGesture
                            val wasSelected = first.mediaStoreId in currentSelection.value
                            val target = !wasSelected
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedIds = toggle(selectedIds, first.mediaStoreId)
                            val visited = mutableSetOf(first.mediaStoreId)

                            var currentY: Float = initialY
                            val scrollJob = scope.launch {
                                while (isActive) {
                                    val y = currentY
                                    val speed = when {
                                        y < edgeZonePx ->
                                            -maxScrollPxPerFrame *
                                                ((edgeZonePx - y) / edgeZonePx)
                                                    .coerceIn(0f, 1f)
                                        y > viewportHeight - edgeZonePx ->
                                            maxScrollPxPerFrame *
                                                ((y - (viewportHeight - edgeZonePx)) /
                                                    edgeZonePx).coerceIn(0f, 1f)
                                        else -> 0f
                                    }
                                    if (speed != 0f) listState.scrollBy(speed)
                                    val t = trackAtY(
                                        listState, currentTracks.value, y,
                                    )
                                    if (t != null && t.mediaStoreId !in visited) {
                                        visited += t.mediaStoreId
                                        val isSel =
                                            t.mediaStoreId in currentSelection.value
                                        if (isSel != target) {
                                            selectedIds = toggle(selectedIds, t.mediaStoreId)
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
            ) {
                items(items = filteredTracks, key = { it.mediaStoreId }) { track ->
                    val selected = track.mediaStoreId in selectedIds
                    val rowBg = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .clickable {
                                selectedIds = toggle(selectedIds, track.mediaStoreId)
                            }
                            .padding(
                                horizontal = Spacing.md,
                                vertical = Spacing.xs,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = {
                                selectedIds = toggle(selectedIds, track.mediaStoreId)
                            },
                        )
                        Column(modifier = Modifier.fillMaxWidth()) {
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
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            FilledTonalButton(
                onClick = { onConfirm(selectedIds) },
                enabled = selectedIds.isNotEmpty(),
            ) {
                Text(text = stringResource(R.string.add_to_queue))
            }
        }
    }
}

private fun toggle(set: Set<Long>, id: Long): Set<Long> =
    if (id in set) set - id else set + id

private fun trackAtY(state: LazyListState, tracks: List<Track>, y: Float): Track? {
    val scrollY = y + state.layoutInfo.viewportStartOffset
    val info = state.layoutInfo.visibleItemsInfo.firstOrNull {
        scrollY >= it.offset && scrollY < it.offset + it.size
    } ?: return null
    return tracks.getOrNull(info.index)
}

@Composable
private fun PickerScaffold(
    title: String,
    onBack: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    isEmpty: Boolean,
    emptyText: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = Spacing.sm),
            )
        }
        PickerSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        )
        HorizontalDivider()
        if (isEmpty) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
            )
        } else {
            // 56dp * ~7 rows + room — bound the height so the sheet stays a sheet
            // rather than swallowing the screen when the user has hundreds of items.
            LazyColumn(modifier = Modifier.height(420.dp), content = content)
        }
    }
}

/**
 * Compact in-sheet search input. Mirrors LibraryScreen's `SearchField` look (rounded
 * pill, surfaceVariant background, leading magnifier, trailing clear-X) so the picker
 * feels like the rest of the app, but kept inline here to avoid coupling the
 * now-playing module to library-internal composables.
 */
@Composable
private fun PickerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val textStyle = LocalTextStyle.current.merge(
        MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
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
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(LocalContentColor.current),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant,
                    )
                }
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
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

private fun filterTracks(tracks: List<Track>, query: String): List<Track> {
    val q = query.trim()
    if (q.isEmpty()) return tracks
    return tracks.filter { t ->
        t.title.contains(q, ignoreCase = true) ||
            (t.artist?.contains(q, ignoreCase = true) == true)
    }
}

private fun filterAlbums(albums: List<Album>, query: String): List<Album> {
    val q = query.trim()
    if (q.isEmpty()) return albums
    return albums.filter { a ->
        a.name.contains(q, ignoreCase = true) ||
            (a.artist?.contains(q, ignoreCase = true) == true)
    }
}

private fun filterPlaylists(playlists: List<Playlist>, query: String): List<Playlist> {
    val q = query.trim()
    if (q.isEmpty()) return playlists
    return playlists.filter { it.name.contains(q, ignoreCase = true) }
}
