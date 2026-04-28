package dev.maxxximgb.genesis.ui.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.PlaylistSummary
import dev.maxxximgb.genesis.ui.components.EmptyState
import dev.maxxximgb.genesis.ui.components.ErrorState
import dev.maxxximgb.genesis.ui.components.LoadingState
import dev.maxxximgb.genesis.ui.components.PlaylistCard
import dev.maxxximgb.genesis.ui.playlists.components.CreatePlaylistDialog
import dev.maxxximgb.genesis.ui.playlists.components.DeletePlaylistDialog
import dev.maxxximgb.genesis.ui.playlists.components.RenamePlaylistDialog
import dev.maxxximgb.genesis.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    snackbarHostState: SnackbarHostState,
    onPlaylistClick: (Long) -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
    var menuFor by remember { mutableStateOf<Playlist?>(null) }

    val createdMsg = stringResource(R.string.snack_playlist_created)
    val renamedMsg = stringResource(R.string.snack_playlist_renamed)
    val deletedMsg = stringResource(R.string.snack_playlist_deleted)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.playlists_title)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_playlist))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is PlaylistsUiState.Loading -> LoadingState()
                is PlaylistsUiState.Error -> ErrorState(message = s.message)
                is PlaylistsUiState.Content -> {
                    if (s.summaries.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.PlaylistAdd,
                            title = stringResource(R.string.empty_playlists_title),
                            subtitle = stringResource(R.string.empty_playlists_subtitle),
                        )
                    } else {
                        SummariesList(
                            summaries = s.summaries,
                            onClick = onPlaylistClick,
                            onLongClick = { menuFor = it },
                        )
                    }
                }
            }
        }
    }

    PlaylistContextMenu(
        target = menuFor,
        onDismiss = { menuFor = null },
        onRename = {
            val playlist = menuFor
            menuFor = null
            renameTarget = playlist
        },
        onDelete = {
            val playlist = menuFor
            menuFor = null
            deleteTarget = playlist
        },
    )

    if (showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    viewModel.create(name)
                    scope.launch { snackbarHostState.showSnackbar(createdMsg) }
                }
                showCreate = false
            },
        )
    }

    val rename = renameTarget
    if (rename != null) {
        RenamePlaylistDialog(
            initial = rename.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                if (newName.isNotBlank() && newName != rename.name) {
                    viewModel.rename(rename.id, newName)
                    scope.launch { snackbarHostState.showSnackbar(renamedMsg) }
                }
                renameTarget = null
            },
        )
    }

    val delete = deleteTarget
    if (delete != null) {
        DeletePlaylistDialog(
            playlistName = delete.name,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.delete(delete.id)
                deleteTarget = null
                scope.launch { snackbarHostState.showSnackbar(deletedMsg) }
            },
        )
    }
}

@Composable
private fun SummariesList(
    summaries: List<PlaylistSummary>,
    onClick: (Long) -> Unit,
    onLongClick: (Playlist) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = summaries, key = { it.playlist.id }) { summary ->
            PlaylistCard(
                playlist = summary.playlist,
                trackCount = summary.trackCount,
                onClick = { onClick(summary.playlist.id) },
                onLongClick = { onLongClick(summary.playlist) },
            )
        }
    }
}

@Composable
private fun PlaylistContextMenu(
    target: Playlist?,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(
        expanded = target != null,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.rename)) },
            onClick = onRename,
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            onClick = onDelete,
        )
    }
}
