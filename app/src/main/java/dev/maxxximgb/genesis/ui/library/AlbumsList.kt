package dev.maxxximgb.genesis.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.Album
import dev.maxxximgb.genesis.ui.components.AlbumArtImage
import dev.maxxximgb.genesis.ui.components.EmptyState
import dev.maxxximgb.genesis.ui.theme.Spacing

@Composable
fun AlbumsList(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
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
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(albums, key = { it.id }) { album ->
            AlbumRow(album = album, onClick = { onAlbumClick(album) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun AlbumRow(album: Album, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
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
            Text(
                text = "${album.artist ?: stringResource(R.string.unknown_artist)} · " +
                    stringResource(R.string.album_track_count, album.trackCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
