package dev.maxxximgb.genesis.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.ui.components.LoadingState
import dev.maxxximgb.genesis.ui.components.TrackRow
import dev.maxxximgb.genesis.ui.nowPlaying.NowPlayingViewModel
import dev.maxxximgb.genesis.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") snackbarHostState: SnackbarHostState,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val album by viewModel.album.collectAsStateWithLifecycle()
    val items = viewModel.pagedTracks.collectAsLazyPagingItems()
    BrowseDetail(
        title = album?.name ?: "",
        subtitle = album?.let { stringResource(R.string.album_track_count, it.trackCount) },
        onNavigateBack = onNavigateBack,
        items = items,
        onPlay = nowPlayingViewModel::playTracks,
    )
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
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
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
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (items.itemCount == 0) {
                LoadingState()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(top = Spacing.xs)) {
                    items(
                        count = items.itemCount,
                        key = items.itemKey { it.mediaStoreId },
                    ) { index ->
                        val track = items[index] ?: return@items
                        TrackRow(
                            track = track,
                            onClick = {
                                val snapshot = items.itemSnapshotList.items
                                val idx = snapshot.indexOfFirst { it.mediaStoreId == track.mediaStoreId }
                                if (idx >= 0) onPlay(snapshot, idx)
                            },
                        )
                    }
                }
            }
        }
    }
}
