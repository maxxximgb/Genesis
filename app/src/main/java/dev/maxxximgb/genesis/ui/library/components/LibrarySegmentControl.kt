package dev.maxxximgb.genesis.ui.library.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.ui.library.LibraryBrowseTab
import dev.maxxximgb.genesis.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySegmentControl(
    current: LibraryBrowseTab,
    onSelect: (LibraryBrowseTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = LibraryBrowseTab.entries
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
    ) {
        tabs.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = tab == current,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                icon = {},
                label = {
                    androidx.compose.material3.Text(
                        text = stringResource(tab.labelRes()),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

private fun LibraryBrowseTab.labelRes(): Int = when (this) {
    LibraryBrowseTab.TRACKS -> R.string.browse_tracks
    LibraryBrowseTab.ALBUMS -> R.string.browse_albums
    LibraryBrowseTab.ARTISTS -> R.string.browse_artists
    LibraryBrowseTab.FOLDERS -> R.string.browse_folders
}
