package dev.maxxximgb.genesis.ui.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.audiofx.EqualizerPreset
import dev.maxxximgb.genesis.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onNavigateBack: () -> Unit,
    viewModel: EqualizerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<EqualizerPreset?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.equalizer_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (!state.capabilities.equalizerSupported) {
            UnsupportedView(
                masterEnabled = state.masterEnabled,
                onToggleMaster = viewModel::toggleMaster,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                MasterRow(
                    enabled = state.masterEnabled,
                    onChange = viewModel::toggleMaster,
                )
                HorizontalDivider(modifier = Modifier.padding(top = Spacing.sm))
            }
            item { SectionHeader(stringResource(R.string.equalizer_section_presets)) }
            item {
                PresetDropdown(
                    presets = state.presets,
                    activePresetId = state.activePresetId,
                    enabled = state.masterEnabled,
                    onSelect = viewModel::selectPreset,
                    onRequestRename = { renameTarget = it },
                    onDelete = { viewModel.deletePreset(it.id) },
                    onCreate = { showSaveDialog = true },
                )
            }
            item { SectionHeader(stringResource(R.string.equalizer_section_bands)) }
            item {
                BandsBlock(
                    bandLevels = state.bandLevelsMillibels,
                    centerFrequenciesHz = state.capabilities.bandCenterFrequenciesHz,
                    rangeMillibels = state.capabilities.bandLevelRangeMillibels,
                    enabled = state.masterEnabled,
                    onBandChange = viewModel::setBand,
                )
                Spacer(modifier = Modifier.height(Spacing.xl))
            }
        }
    }

    if (showSaveDialog) {
        PresetNameDialog(
            title = stringResource(R.string.equalizer_preset_save_title),
            initialName = "",
            validate = { viewModel.validateName(it) },
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                viewModel.saveCurrentAsPreset(name)
                showSaveDialog = false
            },
        )
    }
    val target = renameTarget
    if (target != null) {
        PresetNameDialog(
            title = stringResource(R.string.equalizer_preset_rename_title),
            initialName = target.name,
            validate = { viewModel.validateName(it, ignoreId = target.id) },
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                viewModel.renamePreset(target.id, name)
                renameTarget = null
            },
        )
    }
}

@Composable
private fun MasterRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.equalizer_master),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
    )
}

@Composable
private fun BandsBlock(
    bandLevels: List<Int>,
    centerFrequenciesHz: List<Int>,
    rangeMillibels: IntRange,
    enabled: Boolean,
    onBandChange: (Int, Int) -> Unit,
) {
    val n = bandLevels.size
    if (n == 0) return
    Column(modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.38f)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            for (i in 0 until n) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    VerticalSlider(
                        value = bandLevels[i].toFloat(),
                        valueRange = rangeMillibels.first.toFloat()..rangeMillibels.last.toFloat(),
                        enabled = enabled,
                        onValueChange = { onBandChange(i, it.toInt()) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            for (i in 0 until n) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = formatFrequency(centerFrequenciesHz.getOrNull(i) ?: 0),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDb(bandLevels[i]),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * Vertical slider implemented by rotating a standard horizontal Material 3 Slider 90°
 * counter-clockwise. The custom layout swaps width/height so the rotated child reports its
 * size correctly to the parent — without it, the slider would think it's still horizontal
 * and overflow / get clipped.
 */
@Composable
private fun VerticalSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                // Pretend we have a tall+narrow box of WxH; measure child as HxW so when
                // rotated -90° its visual orientation matches the parent slot.
                val placeable = measurable.measure(
                    androidx.compose.ui.unit.Constraints.fixed(constraints.maxHeight, constraints.maxWidth),
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(
                        x = -((placeable.width - placeable.height) / 2),
                        y = ((placeable.width - placeable.height) / 2),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { rotationZ = -90f },
        )
    }
}

@Composable
private fun UnsupportedView(
    masterEnabled: Boolean,
    onToggleMaster: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        MasterRow(enabled = masterEnabled, onChange = onToggleMaster)
        HorizontalDivider()
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.equalizer_unsupported),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatFrequency(hz: Int): String = when {
    hz <= 0 -> ""
    hz >= 1000 -> "${hz / 1000} kHz"
    else -> "$hz Hz"
}

private fun formatDb(millibels: Int): String {
    val db = millibels / 100
    val sign = if (db > 0) "+" else ""
    return "$sign$db dB"
}
