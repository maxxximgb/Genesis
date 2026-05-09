package dev.maxxximgb.genesis.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.data.preferences.WidgetPreferencesStore
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import dev.maxxximgb.genesis.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetConfigViewModel @Inject constructor(
    observePlaylists: ObservePlaylistsUseCase,
    private val widgetPrefs: WidgetPreferencesStore,
    private val userPreferences: UserPreferencesStore,
    private val widgetUpdater: WidgetUpdater,
    private val mediaLibraryRepository: MediaLibraryRepository,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = observePlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    /**
     * Number of tracks that would feed an audiobook-bound widget (native IS_AUDIOBOOK ∪
     * user overrides). Drives the count badge under the "All audiobooks" row in the picker.
     * Recomputed when overrides or sort change — sort affects ordering, not count, but the
     * combine keeps it consistent with the queue the widget will actually play.
     */
    val audiobookCount: StateFlow<Int> = combine(
        userPreferences.observeLibrarySort(),
        userPreferences.observeAudiobookOverrides(),
    ) { sort, overrides -> sort to overrides }
        .let { paired ->
            flow {
                paired.collect { (sort, overrides) ->
                    emit(mediaLibraryRepository.getAudiobookTracks(sort, overrides.toList()).size)
                }
            }.flowOn(Dispatchers.IO)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = 0,
        )

    private val _isBinding = MutableStateFlow(false)
    val isBinding: StateFlow<Boolean> = _isBinding.asStateFlow()

    private val _selectedBackground =
        MutableStateFlow<WidgetBackgroundChoice>(WidgetBackgroundChoice.Dark)
    val selectedBackground: StateFlow<WidgetBackgroundChoice> = _selectedBackground.asStateFlow()

    private var hydratedFor: Int? = null

    /**
     * Loads the stored choice for [appWidgetId] if any. Otherwise picks a default that mirrors
     * the player: app's theme setting (LIGHT/DARK) wins; if the app is on AUTO, falls back to the
     * device's [isSystemInDarkMode].
     */
    fun hydrateBackground(appWidgetId: Int, isSystemInDarkMode: Boolean) {
        if (hydratedFor == appWidgetId) return
        hydratedFor = appWidgetId
        viewModelScope.launch {
            val stored = widgetPrefs.getStoredBackgroundFor(appWidgetId)
            _selectedBackground.value = stored ?: defaultFromAppTheme(isSystemInDarkMode)
        }
    }

    private suspend fun defaultFromAppTheme(isSystemInDarkMode: Boolean): WidgetBackgroundChoice {
        return when (userPreferences.observeThemeMode().first()) {
            ThemeMode.LIGHT -> WidgetBackgroundChoice.Light
            ThemeMode.DARK -> WidgetBackgroundChoice.Dark
            ThemeMode.AUTO ->
                if (isSystemInDarkMode) WidgetBackgroundChoice.Dark else WidgetBackgroundChoice.Light
        }
    }

    fun onBackgroundChosen(choice: WidgetBackgroundChoice) {
        _selectedBackground.value = choice
    }

    fun bind(appWidgetId: Int, playlistId: Long, onCommitted: () -> Unit) =
        bindTarget(appWidgetId, WidgetTarget.Playlist(playlistId), onCommitted)

    fun bindAudiobooks(appWidgetId: Int, onCommitted: () -> Unit) =
        bindTarget(appWidgetId, WidgetTarget.Audiobooks, onCommitted)

    private fun bindTarget(
        appWidgetId: Int,
        target: WidgetTarget,
        onCommitted: () -> Unit,
    ) {
        if (!_isBinding.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            widgetPrefs.setTarget(appWidgetId, target)
            widgetPrefs.setBackgroundFor(appWidgetId, _selectedBackground.value)
            // For reconfigure, the widget is already bound and updateWhenBound bumps it on the
            // first poll. For a brand-new widget the launcher only finishes binding after this
            // activity returns RESULT_OK, so the polling loop waits for it to appear.
            widgetUpdater.updateWhenBound(appWidgetId)
            onCommitted()
        }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
