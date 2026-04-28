package dev.maxxximgb.genesis.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.domain.usecase.playlist.CreatePlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.DeletePlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistSummariesUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.RenamePlaylistUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    observePlaylistSummaries: ObservePlaylistSummariesUseCase,
    private val createPlaylist: CreatePlaylistUseCase,
    private val renamePlaylist: RenamePlaylistUseCase,
    private val deletePlaylist: DeletePlaylistUseCase,
) : ViewModel() {

    val uiState: StateFlow<PlaylistsUiState> = observePlaylistSummaries()
        .map<_, PlaylistsUiState> { PlaylistsUiState.Content(it) }
        .catch { e -> emit(PlaylistsUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = PlaylistsUiState.Loading,
        )

    fun create(name: String) {
        viewModelScope.launch { createPlaylist(name) }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { renamePlaylist(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { deletePlaylist(id) }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
