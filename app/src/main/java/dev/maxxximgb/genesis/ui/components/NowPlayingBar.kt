package dev.maxxximgb.genesis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.LoopState
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.loopStateOf
import dev.maxxximgb.genesis.ui.theme.Elevation
import dev.maxxximgb.genesis.ui.theme.Sizes
import dev.maxxximgb.genesis.ui.theme.Spacing

private const val DISMISS_THRESHOLD_DP = 48

@Composable
fun NowPlayingBar(
    state: PlaybackState,
    albumId: Long?,
    modifier: Modifier = Modifier,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTap: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { DISMISS_THRESHOLD_DP.dp.toPx() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            .pointerInput(Unit) {
                // Track only downward drag — upward should not dismiss. We accumulate the
                // total drop and trigger on release if it crossed the threshold, so a
                // brief touch wobble doesn't accidentally hide the bar.
                var accumulated = 0f
                detectVerticalDragGestures(
                    onDragStart = { accumulated = 0f },
                    onDragCancel = { accumulated = 0f },
                    onDragEnd = {
                        if (accumulated >= dismissThresholdPx) onDismiss()
                        accumulated = 0f
                    },
                ) { _, dragAmount ->
                    if (dragAmount > 0f) accumulated += dragAmount
                }
            },
        tonalElevation = Elevation.high,
        // surfaceContainerHigh is stepped above surfaceVariant so the floating bar
        // doesn't blend with the library background in dark mode.
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Sizes.nowPlayingBarHeight)
                    .clickable(onClick = onTap)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtImage(
                    albumId = albumId,
                    contentDescription = null,
                    size = Sizes.albumArtMedium,
                    // Smaller placeholder than the library rows — the mini bar's tile is bigger
                    // (medium vs small) so 0.5 looked clunky here. 0.32 keeps the surface mostly
                    // empty so the bar reads quiet when no art is available.
                    placeholderFraction = 0.32f,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.md),
                ) {
                    Text(
                        text = state.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.artist ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Order: [prev] [play/pause] [next]. prev/next stay visible but go disabled
                // (Material's automatic muted tint via `enabled = false`) when the queue has
                // nowhere to navigate — single-track queue, or edges in a non-repeating queue.
                // In repeat-all / repeat-one / shuffle modes navigation always works, so both
                // sides stay enabled.
                val mode = loopStateOf(state.repeatMode, state.shuffleEnabled)
                val canSkip = state.queue.size > 1
                val canPrev = canSkip && (mode != LoopState.OFF || state.currentIndex > 0)
                val canNext = canSkip && (mode != LoopState.OFF ||
                    state.currentIndex < state.queue.lastIndex)
                IconButton(onClick = onPrevious, enabled = canPrev) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.previous),
                        modifier = Modifier.size(28.dp),
                    )
                }
                IconButton(onClick = onTogglePlayPause) {
                    val icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                    val cd = if (state.isPlaying) R.string.pause else R.string.play
                    Icon(
                        icon,
                        contentDescription = stringResource(cd),
                        modifier = Modifier.size(28.dp),
                    )
                }
                IconButton(onClick = onNext, enabled = canNext) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.next),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            ProgressIndicator(state = state)
        }
    }
}

@Composable
private fun BoxScope.ProgressIndicator(state: PlaybackState) {
    if (state.durationMs <= 0L) return
    val fraction = (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
