package dev.maxxximgb.genesis.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.Album
import dev.maxxximgb.genesis.ui.components.AlbumArtImage
import dev.maxxximgb.genesis.ui.components.EmptyState
import dev.maxxximgb.genesis.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val CHECKBOX_ANIM_MS = 220

@Composable
fun AlbumsList(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
    selectedIds: Set<Long> = emptySet(),
    onAlbumLongClick: (Album) -> Unit = {},
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(0.dp),
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
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val edgeZonePx = with(density) { 80.dp.toPx() }
    val maxScrollPxPerFrame = with(density) { 14.dp.toPx() }

    val currentAlbums = rememberUpdatedState(albums)
    val currentSelection = rememberUpdatedState(selectedIds)
    val currentToggle = rememberUpdatedState(onAlbumLongClick)

    val selectionMode = selectedIds.isNotEmpty()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val viewportHeight = size.height.toFloat()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val initialY = down.position.y
                    awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    val first = albumAtY(listState, currentAlbums.value, initialY)
                        ?: return@awaitEachGesture
                    val wasSelected = first.id in currentSelection.value
                    val target = !wasSelected
                    currentToggle.value(first)
                    val visited = mutableSetOf(first.id)

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
                            val a = albumAtY(listState, currentAlbums.value, y)
                            if (a != null && a.id !in visited) {
                                visited += a.id
                                val isSel = a.id in currentSelection.value
                                if (isSel != target) currentToggle.value(a)
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
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumRow(
                album = album,
                selected = album.id in selectedIds,
                selectionMode = selectionMode,
                onClick = { onAlbumClick(album) },
                // Long-press handled by the parent's pointerInput so the SAME gesture can
                // extend into drag-select. Forwarding here would double-toggle the first row.
                onLongClick = {},
            )
            HorizontalDivider()
        }
    }
}

private fun albumAtY(state: LazyListState, albums: List<Album>, y: Float): Album? {
    val scrollY = y + state.layoutInfo.viewportStartOffset
    val info = state.layoutInfo.visibleItemsInfo.firstOrNull {
        scrollY >= it.offset && scrollY < it.offset + it.size
    } ?: return null
    return albums.getOrNull(info.index)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumRow(
    album: Album,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(background)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Same slide-in checkbox affordance as TrackRow — without it, selection mode is
        // signaled only by background tint, which the user wasn't reading as "selected".
        AnimatedVisibility(
            visible = selectionMode,
            enter = slideInHorizontally(tween(CHECKBOX_ANIM_MS)) { -it } +
                expandHorizontally(tween(CHECKBOX_ANIM_MS)) +
                fadeIn(tween(CHECKBOX_ANIM_MS)),
            exit = slideOutHorizontally(tween(CHECKBOX_ANIM_MS)) { -it } +
                shrinkHorizontally(tween(CHECKBOX_ANIM_MS)) +
                fadeOut(tween(CHECKBOX_ANIM_MS)),
        ) {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
        }
        AlbumArtImage(albumId = album.id, contentDescription = null)
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val artist = album.artist?.takeIf { it.isNotBlank() }
            val trackCount = stringResource(R.string.album_track_count, album.trackCount)
            Text(
                text = if (artist != null) "$artist · $trackCount" else trackCount,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
