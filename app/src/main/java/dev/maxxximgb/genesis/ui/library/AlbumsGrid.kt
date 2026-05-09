package dev.maxxximgb.genesis.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.data.media.AlbumArt
import dev.maxxximgb.genesis.domain.model.Album
import dev.maxxximgb.genesis.ui.components.EmptyState
import dev.maxxximgb.genesis.ui.theme.Corner
import dev.maxxximgb.genesis.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun AlbumsGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
    selectedIds: Set<Long> = emptySet(),
    onAlbumLongClick: (Album) -> Unit = {},
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
) {
    if (albums.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Album,
            title = stringResource(R.string.empty_albums_title),
            subtitle = null,
            modifier = modifier,
        )
        return
    }
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val edgeZonePx = with(density) { 80.dp.toPx() }
    val maxScrollPxPerFrame = with(density) { 14.dp.toPx() }

    val currentAlbums = rememberUpdatedState(albums)
    val currentSelection = rememberUpdatedState(selectedIds)
    val currentToggle = rememberUpdatedState(onAlbumLongClick)
    val selectionMode = selectedIds.isNotEmpty()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 132.dp),
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val viewportHeight = size.height.toFloat()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val initialX = down.position.x
                    val initialY = down.position.y
                    awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture

                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val first = albumAtPoint(gridState, currentAlbums.value, initialX, initialY)
                        ?: return@awaitEachGesture
                    val wasSelected = first.id in currentSelection.value
                    val target = !wasSelected
                    currentToggle.value(first)
                    val visited = mutableSetOf(first.id)

                    var currentX: Float = initialX
                    var currentY: Float = initialY
                    val scrollJob = scope.launch {
                        while (isActive) {
                            val x = currentX
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
                            if (speed != 0f) gridState.scrollBy(speed)
                            val a = albumAtPoint(gridState, currentAlbums.value, x, y)
                            if (a != null && a.id !in visited) {
                                visited += a.id
                                val isSel = a.id in currentSelection.value
                                if (isSel != target) currentToggle.value(a)
                            }
                            delay(16)
                        }
                    }

                    try {
                        drag(down.id) { change ->
                            change.consume()
                            currentX = change.position.x
                            currentY = change.position.y
                        }
                    } finally {
                        scrollJob.cancel()
                    }
                }
            },
        state = gridState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.sm + contentPadding.calculateTopPadding(),
            bottom = Spacing.lg + contentPadding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumGridCell(
                album = album,
                selected = album.id in selectedIds,
                selectionMode = selectionMode,
                onClick = { onAlbumClick(album) },
                // Long-press handled by the parent's pointerInput above so the SAME gesture
                // can extend into drag-select. Forwarding here would double-toggle.
                onLongClick = {},
            )
        }
    }
}

private fun albumAtPoint(
    state: LazyGridState,
    albums: List<Album>,
    x: Float,
    y: Float,
): Album? {
    // Pointer Y is in viewport-local coords; LazyGridItemInfo.offset is in scroll-content
    // coords. Difference = -viewportStartOffset. X is shared (no horizontal scroll).
    val scrollY = y + state.layoutInfo.viewportStartOffset
    val info = state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
        x >= item.offset.x && x < item.offset.x + item.size.width &&
            scrollY >= item.offset.y && scrollY < item.offset.y + item.size.height
    } ?: return null
    return albums.getOrNull(info.index)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumGridCell(
    album: Album,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(Corner.md))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    // 3dp primary border around selected tiles — checkbox alone reads as
                    // "tappable affordance" rather than "selected" in a grid layout.
                    if (selected) Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(Corner.md),
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(AlbumArt.uri(album.id))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    else -> Icon(
                        imageVector = Icons.Filled.Album,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Show the badge slot on EVERY cell while selection mode is on so the user
            // sees the selectable affordance immediately, not only after they've picked
            // one. Empty circle for unselected, filled-with-check for selected.
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.xs)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else Color.Black.copy(alpha = 0.4f),
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        val artist = album.artist?.takeIf { it.isNotBlank() }
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
