package dev.maxxximgb.genesis.ui.library.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.SortOrder

@Composable
fun SortMenu(
    current: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        SortOrder.DATE_ADDED_DESC to R.string.sort_date_added_desc,
        SortOrder.TITLE_ASC to R.string.sort_title_asc,
        SortOrder.TITLE_DESC to R.string.sort_title_desc,
        SortOrder.ARTIST_ASC to R.string.sort_artist_asc,
        SortOrder.ALBUM_ASC to R.string.sort_album_asc,
        SortOrder.DURATION_ASC to R.string.sort_duration_asc,
        SortOrder.DURATION_DESC to R.string.sort_duration_desc,
    )
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.sort_by))
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        options.forEach { (order, label) ->
            DropdownMenuItem(
                text = { Text(stringResource(label)) },
                trailingIcon = {
                    if (current == order) Icon(Icons.Filled.Check, contentDescription = null)
                },
                onClick = {
                    onSortSelected(order)
                    expanded = false
                },
            )
        }
    }
}
