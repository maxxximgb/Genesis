@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.maximg.player.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maximg.player.LocalAppContainer
import com.maximg.player.R
import com.maximg.player.data.PlaylistTrackItem
import com.maximg.player.util.formatDuration
import kotlinx.coroutines.launch

fun List<PlaylistTrackItem>.moveSelectedTracksUp(selectedIds: Set<Long>): List<PlaylistTrackItem> {
    val result = toMutableList()
    for (i in 1 until result.size) {
        if (result[i].mediaStoreId in selectedIds && result[i - 1].mediaStoreId !in selectedIds) {
            val t = result[i]
            result[i] = result[i - 1]
            result[i - 1] = t
        }
    }
    return result
}

fun List<PlaylistTrackItem>.moveSelectedTracksDown(selectedIds: Set<Long>): List<PlaylistTrackItem> {
    val result = toMutableList()
    for (i in result.size - 2 downTo 0) {
        if (result[i].mediaStoreId in selectedIds && result[i + 1].mediaStoreId !in selectedIds) {
            val t = result[i]
            result[i] = result[i + 1]
            result[i + 1] = t
        }
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onAddTracks: (Long) -> Unit
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    val playlist by container.playlistRepository.observePlaylist(playlistId)
        .collectAsState(initial = null)
    val tracks by container.playlistRepository.observePlaylistTracks(playlistId)
        .collectAsState(initial = emptyList())

    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = playlist?.name ?: stringResource(R.string.playlist_title_default),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.playlist_detail_subtitle, tracks.size, ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onAddTracks(playlistId) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_tracks)
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                container.playbackController.playPlaylist(playlistId)
                            }
                        },
                        enabled = tracks.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.play)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (isSelectionMode) {
                SelectionActionBar(
                    selectedCount = selectedIds.size,
                    onMoveUp = {
                        val newOrder = tracks.moveSelectedTracksUp(selectedIds)
                        scope.launch {
                            container.playlistRepository.reorderTracks(
                                playlistId,
                                newOrder.map { it.mediaStoreId }
                            )
                        }
                    },
                    onMoveDown = {
                        val newOrder = tracks.moveSelectedTracksDown(selectedIds)
                        scope.launch {
                            container.playlistRepository.reorderTracks(
                                playlistId,
                                newOrder.map { it.mediaStoreId }
                            )
                        }
                    },
                    onDelete = {
                        scope.launch {
                            container.playlistRepository.removeTracksFromPlaylist(
                                playlistId,
                                selectedIds.toList()
                            )
                            selectedIds = emptySet()
                        }
                    },
                    onCancel = { selectedIds = emptySet() }
                )
            }
        }
    ) { padding ->
        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.playlist_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.playlist_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { onAddTracks(playlistId) }) {
                        Text(stringResource(R.string.add_tracks))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(tracks, key = { _, track -> track.mediaStoreId }) { index, track ->
                    TrackRow(
                        track = track,
                        isSelectionMode = isSelectionMode,
                        isSelected = track.mediaStoreId in selectedIds,
                        onPlay = {
                            scope.launch {
                                container.playbackController.playPlaylist(
                                    playlistId,
                                    startIndex = index
                                )
                            }
                        },
                        onLongPress = {
                            selectedIds = selectedIds + track.mediaStoreId
                        },
                        onCheckChange = { checked ->
                            selectedIds = if (checked) {
                                selectedIds + track.mediaStoreId
                            } else {
                                selectedIds - track.mediaStoreId
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: PlaylistTrackItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onPlay: () -> Unit,
    onLongPress: () -> Unit,
    onCheckChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = onLongPress,
                onClick = {
                    if (isSelectionMode) onCheckChange(!isSelected) else onPlay()
                }
            ),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onCheckChange
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (isSelectionMode) 4.dp else 0.dp)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = track.artist ?: stringResource(R.string.unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatDuration(track.durationMs),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp)
            )
        }
    }
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.selected_tracks_count, selectedCount),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onMoveUp) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move Up"
                )
            }
            IconButton(onClick = onMoveDown) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down"
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel)
                )
            }
        }
    }
}
