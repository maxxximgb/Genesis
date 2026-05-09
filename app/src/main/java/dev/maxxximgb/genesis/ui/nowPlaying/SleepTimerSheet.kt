package dev.maxxximgb.genesis.ui.nowPlaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.ui.theme.Spacing
import dev.maxxximgb.genesis.ui.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    currentRemainingMs: Long?,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            Text(
                text = stringResource(R.string.sleep_timer_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (currentRemainingMs != null) {
                Text(
                    text = stringResource(
                        R.string.sleep_timer_remaining,
                        formatDuration(currentRemainingMs),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.md))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Preset(label = stringResource(R.string.sleep_timer_15min), minutes = 15, onPick = onPick)
                Preset(label = stringResource(R.string.sleep_timer_30min), minutes = 30, onPick = onPick)
                Preset(label = stringResource(R.string.sleep_timer_45min), minutes = 45, onPick = onPick)
                Preset(label = stringResource(R.string.sleep_timer_60min), minutes = 60, onPick = onPick)
            }
            if (currentRemainingMs != null) {
                androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.md))
                AssistChip(
                    onClick = onCancel,
                    label = { Text(stringResource(R.string.sleep_timer_off)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.md))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Preset(
    label: String,
    minutes: Int,
    onPick: (Int) -> Unit,
) {
    FilterChip(
        selected = false,
        onClick = { onPick(minutes) },
        label = { Text(label) },
    )
}
