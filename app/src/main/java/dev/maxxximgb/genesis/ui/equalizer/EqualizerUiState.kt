package dev.maxxximgb.genesis.ui.equalizer

import dev.maxxximgb.genesis.domain.audiofx.AudioFxCapabilities
import dev.maxxximgb.genesis.domain.audiofx.EqualizerPreset

data class EqualizerUiState(
    val capabilities: AudioFxCapabilities = AudioFxCapabilities.Unsupported,
    val masterEnabled: Boolean = false,
    val activePresetId: String?,
    val bandLevelsMillibels: List<Int> = emptyList(),
    val presets: List<EqualizerPreset> = emptyList(),
)
