package dev.maxxximgb.genesis.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.data.preferences.WidgetPreferencesStore
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetConfigViewModel @Inject constructor(
    observePlaylists: ObservePlaylistsUseCase,
    private val widgetPrefs: WidgetPreferencesStore,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = observePlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    private val _isBinding = MutableStateFlow(false)
    val isBinding: StateFlow<Boolean> = _isBinding.asStateFlow()

    private val _selectedBackground =
        MutableStateFlow<WidgetBackgroundChoice>(WidgetBackgroundChoice.Dynamic)
    val selectedBackground: StateFlow<WidgetBackgroundChoice> = _selectedBackground.asStateFlow()

    private var hydratedFor: Int? = null

    fun hydrateBackground(appWidgetId: Int) {
        if (hydratedFor == appWidgetId) return
        hydratedFor = appWidgetId
        viewModelScope.launch {
            _selectedBackground.value = widgetPrefs.getBackgroundFor(appWidgetId)
        }
    }

    fun onBackgroundChosen(choice: WidgetBackgroundChoice) {
        _selectedBackground.value = choice
    }

    fun bind(appWidgetId: Int, playlistId: Long, onCommitted: () -> Unit) {
        if (!_isBinding.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            widgetPrefs.setPlaylistFor(appWidgetId, playlistId)
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
