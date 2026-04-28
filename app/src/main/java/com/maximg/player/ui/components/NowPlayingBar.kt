package com.maximg.player.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maximg.player.LocalAppContainer
import com.maximg.player.R
import com.maximg.player.playback.PlaybackState
import kotlinx.coroutines.launch

@Composable
fun NowPlayingBar() {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val state by container.playbackStateStore.stateFlow.collectAsState(
        initial = PlaybackState(
            isPlaying = false,
            title = null,
            artist = null,
            shuffle = false,
            repeatMode = 0,
            canGoPrevious = false,
            canGoNext = false,
            playlistId = null
        )
    )

    if (state.title == null) return

    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title ?: stringResource(R.string.nothing_playing),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.artist != null) {
                        Text(
                            text = state.artist!!,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = { scope.launch { container.playbackController.previous() } },
                    enabled = state.canGoPrevious
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.previous)
                    )
                }
                IconButton(
                    onClick = { scope.launch { container.playbackController.togglePlayPause() } }
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.play_pause)
                    )
                }
                IconButton(
                    onClick = { scope.launch { container.playbackController.next() } },
                    enabled = state.canGoNext
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.next)
                    )
                }
            }
        }
    }
}
