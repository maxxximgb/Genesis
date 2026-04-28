package dev.maxxximgb.genesis.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ThemeState(
    val mode: ThemeMode = ThemeMode.DEFAULT,
    val palette: PaletteId = PaletteId.DEFAULT,
    val fontScale: Float = UserPreferencesStore.DEFAULT_FONT_SCALE,
)

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferences: UserPreferencesStore,
) : ViewModel() {

    val themeState: StateFlow<ThemeState> = combine(
        preferences.observeThemeMode(),
        preferences.observePalette(),
        preferences.observeFontScale(),
    ) { mode, palette, scale -> ThemeState(mode, palette, scale) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeState(),
        )
}
