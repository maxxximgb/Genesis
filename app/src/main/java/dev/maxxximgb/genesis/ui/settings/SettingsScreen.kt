package dev.maxxximgb.genesis.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.data.locale.LocaleController
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.ui.theme.Spacing
import dev.maxxximgb.genesis.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item { ThemeSection(state.themeMode, viewModel::setThemeMode) }
            item { LanguageSection(state.language, viewModel::setLanguage) }
            item { FontScaleSection(state.fontScale, viewModel::setFontScale) }
            if (state.equalizerSupported) {
                item {
                    AudioSection(
                        equalizerEnabled = state.equalizerEnabled,
                        onNavigateToEqualizer = onNavigateToEqualizer,
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioSection(
    equalizerEnabled: Boolean,
    onNavigateToEqualizer: () -> Unit,
) {
    SectionHeader(stringResource(R.string.pref_section_audio))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToEqualizer)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EqualizerIndicatorIcon(active = equalizerEnabled)
        Text(
            text = stringResource(R.string.equalizer_title),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(start = Spacing.md)
                .weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * GraphicEq icon with a small dot beneath when EQ is active. Subtle indicator — the icon
 * shape stays consistent, the dot just signals on/off without dominating the row.
 */
@Composable
private fun EqualizerIndicatorIcon(active: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (active) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
            )
        }
    }
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
private fun LanguageSection(
    current: LocaleController.Language,
    onChange: (LocaleController.Language) -> Unit,
) {
    SectionHeader(stringResource(R.string.pref_language))
    val options = listOf(
        LocaleController.Language.SYSTEM to R.string.pref_language_system,
        LocaleController.Language.ENGLISH to R.string.pref_language_english,
        LocaleController.Language.RUSSIAN to R.string.pref_language_russian,
        LocaleController.Language.SPANISH to R.string.pref_language_spanish,
        LocaleController.Language.CHINESE to R.string.pref_language_chinese,
        LocaleController.Language.GERMAN to R.string.pref_language_german,
        LocaleController.Language.FRENCH to R.string.pref_language_french,
        LocaleController.Language.PORTUGUESE to R.string.pref_language_portuguese,
        LocaleController.Language.JAPANESE to R.string.pref_language_japanese,
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
