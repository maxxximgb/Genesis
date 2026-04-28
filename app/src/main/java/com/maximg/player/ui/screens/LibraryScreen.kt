@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.maximg.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.maximg.player.LocalAppContainer
import com.maximg.player.R
import com.maximg.player.data.PlaylistEntity
import com.maximg.player.media.MediaTrack
import com.maximg.player.util.FONT_SCALE_KEY
import com.maximg.player.util.formatDuration
import com.maximg.player.util.uiDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LibraryScreen(
    addToPlaylistId: Long?,
    onAddedToPlaylist: () -> Unit
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Selection mode state (when adding tracks to a playlist via PlaylistDetailScreen)
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Browse selection mode state (multi-select in Library browse mode)
    var browseSelectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isBrowseSelectionMode = addToPlaylistId == null && browseSelectedIds.isNotEmpty()

    // "Add to playlist" dialog state (browse mode)
    // tracksToAdd is non-empty when the dialog is open
    var tracksToAdd by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    val showAddDialog = tracksToAdd.isNotEmpty()

    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        isLoading = true
        delay(300L)
        tracks = container.mediaStoreRepository.queryTracks(searchQuery.ifBlank { null })
        isLoading = false
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            if (addToPlaylistId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onAddedToPlaylist) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                    Text(
                        text = stringResource(R.string.add_tracks),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else if (isBrowseSelectionMode) {
                // Browse selection mode header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { browseSelectedIds = emptySet() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel)
                            )
                        }
                        Text(
                            text = stringResource(R.string.selected_tracks_count, browseSelectedIds.size),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = {
                            tracksToAdd = tracks.filter { it.mediaStoreId in browseSelectedIds }
                        }) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = stringResource(R.string.add_to_playlist),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 4.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.library_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.library_subtitle, tracks.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.large
            )

            // Track list
            if (!isLoading && tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (searchQuery.isBlank())
                                stringResource(R.string.library_empty_title)
                            else
                                stringResource(R.string.library_empty_search_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (searchQuery.isBlank())
                                stringResource(R.string.library_empty_subtitle)
                            else
                                stringResource(R.string.library_empty_search_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val bottomPad = if (addToPlaylistId != null) 80.dp else 16.dp
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 0.dp,
                        end = 0.dp,
                        top = 4.dp,
                        bottom = bottomPad
                    )
                ) {
                    items(tracks, key = { it.mediaStoreId }) { track ->
                        if (addToPlaylistId != null) {
                            // Playlist-add selection mode row
                            SelectableTrackRow(
                                track = track,
                                isSelected = track.mediaStoreId in selectedIds,
                                onToggle = {
                                    selectedIds = if (track.mediaStoreId in selectedIds) {
                                        selectedIds - track.mediaStoreId
                                    } else {
                                        selectedIds + track.mediaStoreId
                                    }
                                }
                            )
                        } else {
                            // Browse mode row (supports single-tap add and multi-select)
                            BrowseTrackRow(
                                track = track,
                                isSelectionMode = isBrowseSelectionMode,
                                isSelected = track.mediaStoreId in browseSelectedIds,
                                onAddToPlaylist = {
                                    tracksToAdd = listOf(track)
                                },
                                onToggleSelection = {
                                    browseSelectedIds = if (track.mediaStoreId in browseSelectedIds) {
                                        browseSelectedIds - track.mediaStoreId
                                    } else {
                                        browseSelectedIds + track.mediaStoreId
                                    }
                                },
                                onLongPress = {
                                    browseSelectedIds = browseSelectedIds + track.mediaStoreId
                                }
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // "Add N tracks" button in playlist-add selection mode
                if (addToPlaylistId != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        Button(
                            onClick = {
                                val selected = tracks.filter { it.mediaStoreId in selectedIds }
                                scope.launch {
                                    container.playlistRepository.addTracksToPlaylist(
                                        addToPlaylistId,
                                        selected
                                    )
                                    withContext(Dispatchers.Main.immediate) { onAddedToPlaylist() }
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = if (selectedIds.isEmpty())
                                    stringResource(R.string.add_tracks)
                                else
                                    stringResource(R.string.add) + " ${selectedIds.size}"
                            )
                        }
                    }
                }
            }
        }
    }

    // "Add to playlist" dialog (browse mode)
    if (showAddDialog) {
        AddToPlaylistDialog(
            onAddToPlaylist = { playlistId ->
                val toAdd = tracksToAdd
                scope.launch {
                    container.playlistRepository.addTracksToPlaylist(playlistId, toAdd)
                }
                tracksToAdd = emptyList()
                browseSelectedIds = emptySet()
            },
            onCreateAndAdd = { name ->
                val toAdd = tracksToAdd
                scope.launch {
                    val newId = container.playlistRepository.createPlaylist(name)
                    container.playlistRepository.addTracksToPlaylist(newId, toAdd)
                }
                tracksToAdd = emptyList()
                browseSelectedIds = emptySet()
            },
            onDismiss = {
                tracksToAdd = emptyList()
            }
        )
    }

    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }
}

@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fontScale by context.uiDataStore.data
        .map { it[FONT_SCALE_KEY] ?: 1.0f }
        .collectAsState(initial = 1.0f)

    val options = listOf(
        R.string.ui_size_small to 0.85f,
        R.string.ui_size_normal to 1.0f,
        R.string.ui_size_large to 1.15f,
        R.string.ui_size_xlarge to 1.3f
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_size)) },
        text = {
            Column {
                options.forEach { (labelRes, scale) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    context.uiDataStore.edit { prefs ->
                                        prefs[FONT_SCALE_KEY] = scale
                                    }
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = fontScale == scale,
                            onClick = {
                                scope.launch {
                                    context.uiDataStore.edit { prefs ->
                                        prefs[FONT_SCALE_KEY] = scale
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun BrowseTrackRow(
    track: MediaTrack,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onAddToPlaylist: () -> Unit,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = if (isSelectionMode) onToggleSelection else { {} },
                onLongClick = onLongPress
            )
            .padding(start = if (isSelectionMode) 4.dp else 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = if (isSelectionMode) 4.dp else 0.dp)) {
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
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        if (!isSelectionMode) {
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = stringResource(R.string.add_to_playlist),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SelectableTrackRow(
    track: MediaTrack,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle)
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddToPlaylistDialog(
    onAddToPlaylist: (Long) -> Unit,
    onCreateAndAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val container = LocalAppContainer.current
    val playlists by container.playlistRepository.observePlaylists()
        .collectAsState(initial = emptyList())

    var showNewPlaylistField by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            Column {
                // New playlist option — always at top
                if (!showNewPlaylistField) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = { showNewPlaylistField = true })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.new_playlist),
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text(stringResource(R.string.new_playlist)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val name = newPlaylistName.trim()
                                if (name.isNotBlank()) { onCreateAndAdd(name) }
                            }),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val name = newPlaylistName.trim()
                                if (name.isNotBlank()) { onCreateAndAdd(name) }
                            },
                            enabled = newPlaylistName.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.save),
                                tint = if (newPlaylistName.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Existing playlists
                if (playlists.isEmpty()) {
                    Text(
                        text = stringResource(R.string.playlists_empty_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    playlists.forEach { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            onClick = { onAddToPlaylist(playlist.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun PlaylistRow(
    playlist: PlaylistEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.QueueMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = playlist.name,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
