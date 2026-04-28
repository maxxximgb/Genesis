package dev.maxxximgb.genesis.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.data.locale.LocaleController
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.ui.theme.Corner
import dev.maxxximgb.genesis.ui.theme.PaletteId
import dev.maxxximgb.genesis.ui.theme.Spacing
import dev.maxxximgb.genesis.ui.theme.ThemeMode

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                item { ThemeSection(state.themeMode, viewModel::setThemeMode) }
                item { PaletteSection(state.palette, viewModel::setPalette) }
                item { LanguageSection(state.language, viewModel::setLanguage) }
                item { FontScaleSection(state.fontScale, viewModel::setFontScale) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun ThemeSection(current: ThemeMode, onChange: (ThemeMode) -> Unit) {
    SectionHeader(stringResource(R.string.pref_theme))
    val options = listOf(
        ThemeMode.AUTO to R.string.pref_theme_auto,
        ThemeMode.LIGHT to R.string.pref_theme_light,
        ThemeMode.DARK to R.string.pref_theme_dark,
    )
    options.forEach { (mode, label) ->
        RadioRow(
            selected = current == mode,
            text = stringResource(label),
            onClick = { onChange(mode) },
        )
    }
}

@Composable
private fun PaletteSection(current: PaletteId, onChange: (PaletteId) -> Unit) {
    SectionHeader(stringResource(R.string.pref_palette))
    PaletteId.entries.forEach { palette ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChange(palette) }
                .padding(vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = current == palette, onClick = { onChange(palette) })
            PaletteSwatch(palette)
            Text(
                text = stringResource(palette.displayResId),
                modifier = Modifier.padding(start = Spacing.md),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PaletteSwatch(palette: PaletteId) {
    val color = palette.seed
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}

@Composable
private fun LanguageSection(
    current: LocaleController.Language,
    onChange: (LocaleController.Language) -> Unit,
) {
    SectionHeader(stringResource(R.string.pref_language))
    val options = listOf(
        LocaleController.Language.SYSTEM to R.string.pref_language_system,
        LocaleController.Language.ENGLISH to R.string.pref_language_english,
        LocaleController.Language.RUSSIAN to R.string.pref_language_russian,
    )
    options.forEach { (lang, label) ->
        RadioRow(
            selected = current == lang,
            text = stringResource(label),
            onClick = { onChange(lang) },
        )
    }
}

@Composable
private fun FontScaleSection(current: Float, onChange: (Float) -> Unit) {
    SectionHeader(stringResource(R.string.pref_font_scale))
    val labels = mapOf(
        0.85f to R.string.font_scale_small,
        1.0f to R.string.font_scale_normal,
        1.15f to R.string.font_scale_large,
        1.30f to R.string.font_scale_xlarge,
    )
    UserPreferencesStore.FONT_SCALE_OPTIONS.forEach { scale ->
        RadioRow(
            selected = current == scale,
            text = stringResource(labels[scale] ?: R.string.font_scale_normal),
            onClick = { onChange(scale) },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = Spacing.xs),
    )
}

@Composable
private fun RadioRow(selected: Boolean, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = text,
            modifier = Modifier.padding(start = Spacing.md),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
