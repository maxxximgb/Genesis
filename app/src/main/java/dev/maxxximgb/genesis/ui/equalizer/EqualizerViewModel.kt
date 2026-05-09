package dev.maxxximgb.genesis.ui.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.data.audiofx.AudioFxController
import dev.maxxximgb.genesis.data.audiofx.EqualizerPresetRepository
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.domain.audiofx.EqualizerPreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val preferences: UserPreferencesStore,
    private val presetRepository: EqualizerPresetRepository,
    audioFxController: AudioFxController,
) : ViewModel() {

    init {
        // Don't wait for the player to publish an audio session — probe capabilities up front
        // so the screen renders correctly even when the user opens it before any track plays.
        audioFxController.probeCapabilitiesIfNeeded()
    }

    val uiState: StateFlow<EqualizerUiState> = combine(
        preferences.observeAudioFxState(),
        presetRepository.observePresets(),
        audioFxController.capabilities,
    ) { state, presets, caps ->
        // Pad/truncate persisted bands to match the device's actual band count. Saved presets
        // keep their full original length untouched in the repo — only what we display here
        // is normalized to capabilities.
        val normalizedBands = if (caps.equalizerSupported) {
            normalizeBands(state.bandLevelsMillibels, caps.numberOfBands)
        } else state.bandLevelsMillibels
        EqualizerUiState(
            capabilities = caps,
            masterEnabled = state.masterEnabled,
            activePresetId = state.activePresetId,
            bandLevelsMillibels = normalizedBands,
            presets = presets,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = EqualizerUiState(activePresetId = null),
    )

    fun toggleMaster(enabled: Boolean) {
        viewModelScope.launch { preferences.setAudioFxEnabled(enabled) }
    }

    fun setBand(index: Int, millibels: Int) {
        viewModelScope.launch {
            val current = uiState.value
            val caps = current.capabilities
            val n = caps.numberOfBands
            if (n <= 0 || index !in 0 until n) return@launch
            val updated = normalizeBands(current.bandLevelsMillibels, n).toMutableList().also {
                it[index] = millibels.coerceIn(
                    caps.bandLevelRangeMillibels.first,
                    caps.bandLevelRangeMillibels.last,
                )
            }
            preferences.setAudioFxBands(updated)
            persistIntoActivePreset(current) { it.copy(bandLevelsMillibels = updated) }
        }
    }

    /**
     * Persist the slider edit back into the active preset. When [activePresetId] is null we
     * resolve to [EqualizerPreset.DEFAULT_ID] — the default preset is a real, modifiable
     * record (synthesized fresh on first read by the repository), so edits to it survive
     * preset switching the same way edits to user presets do.
     */
    private suspend fun persistIntoActivePreset(
        snapshot: EqualizerUiState,
        transform: (EqualizerPreset) -> EqualizerPreset,
    ) {
        val activeId = snapshot.activePresetId ?: EqualizerPreset.DEFAULT_ID
        val active = snapshot.presets.firstOrNull { it.id == activeId }
            ?: EqualizerPreset.flatDefault(snapshot.capabilities.numberOfBands)
        presetRepository.savePreset(transform(active))
    }

    /**
     * Selects a preset (saved user preset OR the default preset) and loads its band snapshot
     * into the live audio-fx state. Passing null is treated as selecting the default preset.
     */
    fun selectPreset(presetId: String?) {
        viewModelScope.launch {
            val state = uiState.value
            val caps = state.capabilities
            val resolvedId = presetId ?: EqualizerPreset.DEFAULT_ID
            val preset = state.presets.firstOrNull { it.id == resolvedId }
                ?: EqualizerPreset.flatDefault(caps.numberOfBands.coerceAtLeast(0))
            // null in the persisted active id means "default" so dropdown selected-state logic
            // stays consistent across app restarts even if DEFAULT_ID changes in the future.
            // Saved user presets keep their UUID id verbatim.
            val activeIdToStore = if (preset.id == EqualizerPreset.DEFAULT_ID) null else preset.id
            preferences.applyAudioFxPresetSnapshot(
                activePresetId = activeIdToStore,
                bands = normalizeBands(preset.bandLevelsMillibels, caps.numberOfBands.coerceAtLeast(0)),
            )
        }
    }

    /** Saves CURRENT band state as a new preset under [name]. */
    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val current = uiState.value
            val saved = presetRepository.savePreset(
                EqualizerPreset(
                    id = UUID.randomUUID().toString(),
                    name = name.trim().take(EqualizerPreset.MAX_NAME_LENGTH),
                    bandLevelsMillibels = current.bandLevelsMillibels,
                ),
            )
            preferences.setAudioFxActivePresetId(saved.id)
        }
    }

    fun renamePreset(id: String, newName: String) {
        viewModelScope.launch {
            presetRepository.renamePreset(id, newName.trim().take(EqualizerPreset.MAX_NAME_LENGTH))
        }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch {
            presetRepository.deletePreset(id)
            // If the deleted preset was active, fall back to the default preset.
            if (uiState.value.activePresetId == id) {
                preferences.setAudioFxActivePresetId(null)
            }
        }
    }

    /**
     * Validates a new preset name in [PresetNameDialog]. Trim+truncate happens on save; this
     * just classifies what to show under the input field.
     */
    fun validateName(name: String, ignoreId: String? = null): NameValidation {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return NameValidation.Empty
        val collides = uiState.value.presets.any { it.id != ignoreId && it.name.equals(trimmed, ignoreCase = true) }
        return if (collides) NameValidation.Duplicate else NameValidation.Valid
    }

    enum class NameValidation { Valid, Empty, Duplicate }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

        fun normalizeBands(bands: List<Int>, n: Int): List<Int> {
            if (n <= 0) return emptyList()
            return List(n) { i -> bands.getOrNull(i) ?: 0 }
        }
    }
}
