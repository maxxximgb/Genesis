package dev.maxxximgb.genesis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.ui.theme.Corner
import dev.maxxximgb.genesis.ui.theme.Sizes
import dev.maxxximgb.genesis.ui.theme.Spacing

private const val SELECTION_ANIM_MS = 220

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistCard(
    playlist: Playlist,
    trackCount: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onEnqueue: (() -> Unit)? = null,
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(Corner.lg),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.xs, top = Spacing.sm, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Checkbox slides in from the left when entering selection mode — same easing
            // as TrackRow so both lists feel like a single coordinated chrome change.
            AnimatedVisibility(
                visible = selectionMode,
                enter = slideInHorizontally(tween(SELECTION_ANIM_MS)) { -it } +
                    expandHorizontally(tween(SELECTION_ANIM_MS)) +
                    fadeIn(tween(SELECTION_ANIM_MS)),
                exit = slideOutHorizontally(tween(SELECTION_ANIM_MS)) { -it } +
                    shrinkHorizontally(tween(SELECTION_ANIM_MS)) +
                    fadeOut(tween(SELECTION_ANIM_MS)),
            ) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = Spacing.xs),
                )
            }
            Icon(
                imageVector = Icons.Filled.PlaylistPlay,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(Sizes.albumArtSmall)
                    .clip(RoundedCornerShape(Corner.sm)),
            )
            Column(modifier = Modifier.padding(start = Spacing.md).weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.track_count, trackCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Kebab slides out to the right while in selection mode — actions are bulk
            // ones from the top bar in that mode, so per-row menu would just be noise.
            AnimatedVisibility(
                visible = !selectionMode &&
                    (onRename != null || onDelete != null || onEnqueue != null),
                enter = slideInHorizontally(tween(SELECTION_ANIM_MS)) { it } +
                    expandHorizontally(tween(SELECTION_ANIM_MS)) +
                    fadeIn(tween(SELECTION_ANIM_MS)),
                exit = slideOutHorizontally(tween(SELECTION_ANIM_MS)) { it } +
                    shrinkHorizontally(tween(SELECTION_ANIM_MS)) +
                    fadeOut(tween(SELECTION_ANIM_MS)),
            ) {
                PlaylistOverflow(
                    onRename = onRename,
                    onDelete = onDelete,
                    onEnqueue = onEnqueue,
                )
            }
        }
    }
}

@Composable
private fun PlaylistOverflow(
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onEnqueue: (() -> Unit)?,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.more),
        )
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        if (onEnqueue != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_to_queue)) },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
                },
                onClick = {
                    open = false
                    onEnqueue()
                },
            )
        }
        if (onRename != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename)) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = {
                    open = false
                    onRename()
                },
            )
        }
        if (onDelete != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = {
                    open = false
                    onDelete()
                },
            )
        }
    }
}
