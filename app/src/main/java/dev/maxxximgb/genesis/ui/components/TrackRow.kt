package dev.maxxximgb.genesis.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.ui.theme.Spacing
import kotlinx.coroutines.launch

data class TrackAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    isCurrentlyPlaying: Boolean = false,
    actions: List<TrackAction> = emptyList(),
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surface
    val primaryTextColor = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = backgroundColor,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NowPlayingAccent(visible = isCurrentlyPlaying)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (selectionMode) {
                    Checkbox(checked = selected, onCheckedChange = { onClick() })
                }
                AlbumArtImage(albumId = track.albumId, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = trackSubtitle(track),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (actions.isNotEmpty() && !selectionMode) {
                    TrackOverflowMenu(actions = actions)
                }
            }
        }
    }
}

@Composable
private fun NowPlayingAccent(visible: Boolean) {
    val color = if (visible) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(48.dp)
            .background(color),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackOverflowMenu(actions: List<TrackAction>) {
    var open by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    IconButton(onClick = { open = true }) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.track_more_actions),
        )
    }

    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheetState,
        ) {
            actions.forEach { action ->
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                sheetState.hide()
                                open = false
                                action.onClick()
                            }
                        },
                    leadingContent = {
                        Icon(action.icon, contentDescription = null)
                    },
                    headlineContent = { Text(action.label) },
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }
    }
}
