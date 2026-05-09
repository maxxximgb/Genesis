package dev.maxxximgb.genesis.ui.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.audiofx.EqualizerPreset
import dev.maxxximgb.genesis.ui.theme.Spacing

@Composable
fun PresetDropdown(
    presets: List<EqualizerPreset>,
    activePresetId: String?,
    enabled: Boolean,
    onSelect: (String?) -> Unit,
    onRequestRename: (EqualizerPreset) -> Unit,
    onDelete: (EqualizerPreset) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val defaultLabel = stringResource(R.string.equalizer_preset_default)
    val activePresetName = activePresetId
        ?.let { id -> presets.firstOrNull { it.id == id }?.name }
        ?: defaultLabel

    Box(modifier = modifier.alpha(if (enabled) 1f else 0.38f)) {
        OutlinedCard(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            shape = RoundedCornerShape(Spacing.md),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = activePresetName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            // Default preset is always first. It's a real, modifiable record but its display
            // name comes from the localized resource (so it follows the user's language) and
            // it has no rename/delete affordances.
            for (preset in presets) {
                val isDefault = preset.isDefault
                val isActive = if (isDefault) activePresetId == null else preset.id == activePresetId
                val displayName = if (isDefault) defaultLabel else preset.name
                DropdownMenuItem(
                    text = {
                        Text(
                            text = displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        if (isActive) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        } else {
                            Box(modifier = Modifier.height(24.dp))
                        }
                    },
                    trailingIcon = if (isDefault) null else {
                        {
                            // Children (the IconButtons) consume click events first, so taps on
                            // ✎/🗑 do NOT bubble up to the row's click handler — the dropdown
                            // stays open and only the action fires.
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onRequestRename(preset) }) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.rename),
                                    )
                                }
                                IconButton(onClick = { onDelete(preset) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        onSelect(if (isDefault) null else preset.id)
                        expanded = false
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.equalizer_preset_new)) },
                leadingIcon = {
                    Icon(Icons.Filled.Add, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onCreate()
                },
            )
        }
    }
}
