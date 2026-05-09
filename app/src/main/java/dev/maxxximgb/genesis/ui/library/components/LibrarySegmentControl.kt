package dev.maxxximgb.genesis.ui.library.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.ui.library.LibraryBrowseTab
import dev.maxxximgb.genesis.ui.theme.Spacing

private const val ANIM_MS = 200

/**
 * Plain tab bar — all four tabs always visible, the active one filled with primary.
 * Chips wrap to their content (no weight stretch) so long localised labels like
 * "Аудиокниги" don't break onto two lines, and the row scrolls horizontally if the
 * total width exceeds the viewport.
 */
@Composable
fun LibrarySegmentControl(
    current: LibraryBrowseTab,
    onSelect: (LibraryBrowseTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibraryBrowseTab.entries.forEach { tab ->
            FilterChip(
                tab = tab,
                selected = current == tab,
                onClick = { if (current != tab) onSelect(tab) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    tab: LibraryBrowseTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetContainer = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val targetContent = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    val container by animateColorAsState(
        targetValue = targetContainer,
        animationSpec = tween(ANIM_MS),
        label = "chipContainer",
    )
    val content by animateColorAsState(
        targetValue = targetContent,
        animationSpec = tween(ANIM_MS),
        label = "chipContent",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(tab.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            color = content,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

private fun LibraryBrowseTab.labelRes(): Int = when (this) {
    LibraryBrowseTab.TRACKS -> R.string.browse_tracks
    LibraryBrowseTab.ALBUMS -> R.string.browse_albums
    LibraryBrowseTab.PLAYLISTS -> R.string.browse_playlists
    LibraryBrowseTab.AUDIOBOOKS -> R.string.browse_audiobooks
}
