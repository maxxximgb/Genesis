package dev.maxxximgb.genesis.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.data.audiofx.AudioFxController
import dev.maxxximgb.genesis.data.locale.LocaleController
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val fontScale: Float = UserPreferencesStore.DEFAULT_FONT_SCALE,
    val language: LocaleController.Language = LocaleController.Language.SYSTEM,
    // Initial value true: optimistic — assume the device supports EQ until the probe proves
    // otherwise. Avoids momentarily showing/hiding the Audio section while the probe runs on
    // first open. On unsupported hardware the section disappears within a frame.
    val equalizerSupported: Boolean = true,
    val equalizerEnabled: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesStore,
    private val locale: LocaleController,
    audioFxController: AudioFxController,
) : ViewModel() {

    init {
        // Settings is one of the entry points to the EQ — probe so the row can hide itself
        // right away on devices that don't support it.
        audioFxController.probeCapabilitiesIfNeeded()
    }

    private val languageFlow = MutableStateFlow(locale.current())

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.observeThemeMode(),
        preferences.observeFontScale(),
        languageFlow,
        audioFxController.capabilities,
        preferences.observeAudioFxState(),
    ) { mode, scale, lang, caps, fx ->
        SettingsUiState(
            themeMode = mode,
            fontScale = scale,
            language = lang,
            equalizerSupported = caps.equalizerSupported,
            equalizerEnabled = fx.masterEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = SettingsUiState(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { preferences.setFontScale(scale) }
    }

    fun setLanguage(language: LocaleController.Language) {
        locale.setLanguage(language)
        languageFlow.update { language }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
