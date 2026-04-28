package dev.maxxximgb.genesis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.ui.theme.Elevation
import dev.maxxximgb.genesis.ui.theme.Sizes
import dev.maxxximgb.genesis.ui.theme.Spacing

@Composable
fun NowPlayingBar(
    state: PlaybackState,
    modifier: Modifier = Modifier,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTap: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = Elevation.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtImage(
                albumId = null, // 1.5: derived from currentMediaStoreId in 2.3 fullscreen
                contentDescription = null,
                size = Sizes.albumArtSmall,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.md),
            ) {
                Text(
                    text = state.title.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.artist ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onPrevious, enabled = state.queue.size > 1) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.previous))
            }
            IconButton(onClick = onTogglePlayPause) {
                val icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                val cd = if (state.isPlaying) R.string.pause else R.string.play
                Icon(icon, contentDescription = stringResource(cd))
            }
            IconButton(onClick = onNext, enabled = state.queue.size > 1) {
                Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.next))
            }
        }
    }
}
