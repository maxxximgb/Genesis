package dev.maxxximgb.genesis.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.data.locale.LocaleController
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.ui.theme.PaletteId
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
    val palette: PaletteId = PaletteId.DEFAULT,
    val fontScale: Float = UserPreferencesStore.DEFAULT_FONT_SCALE,
    val language: LocaleController.Language = LocaleController.Language.SYSTEM,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesStore,
    private val locale: LocaleController,
) : ViewModel() {

    private val languageFlow = MutableStateFlow(locale.current())

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.observeThemeMode(),
        preferences.observePalette(),
        preferences.observeFontScale(),
        languageFlow,
    ) { mode, palette, scale, lang ->
        SettingsUiState(mode, palette, scale, lang)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = SettingsUiState(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setPalette(palette: PaletteId) {
        viewModelScope.launch { preferences.setPalette(palette) }
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
