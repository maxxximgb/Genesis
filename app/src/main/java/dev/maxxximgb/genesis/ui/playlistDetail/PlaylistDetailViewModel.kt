package dev.maxxximgb.genesis.ui.playlistDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.usecase.playback.PlayPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.AddTracksToPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistDetailUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.RemoveTracksFromPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ReorderPlaylistTracksUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePlaylistDetail: ObservePlaylistDetailUseCase,
    private val addTracksUseCase: AddTracksToPlaylistUseCase,
    private val removeTracksUseCase: RemoveTracksFromPlaylistUseCase,
    private val reorderTracksUseCase: ReorderPlaylistTracksUseCase,
    private val playPlaylistUseCase: PlayPlaylistUseCase,
) : ViewModel() {

    val playlistId: Long = checkNotNull(savedStateHandle["playlistId"]) {
        "PlaylistDetailViewModel requires `playlistId` in SavedStateHandle"
    }

    val uiState: StateFlow<PlaylistDetailUiState> = observePlaylistDetail(playlistId)
        .map<_, PlaylistDetailUiState> { detail ->
            if (detail == null) PlaylistDetailUiState.NotFound
            else PlaylistDetailUiState.Content(detail)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = PlaylistDetailUiState.Loading,
        )

    fun addTracks(tracks: List<Track>) {
        viewModelScope.launch { addTracksUseCase(playlistId, tracks) }
    }

    fun removeTracks(mediaStoreIds: List<Long>) {
        viewModelScope.launch { removeTracksUseCase(playlistId, mediaStoreIds) }
    }

    fun reorder(orderedMediaIds: List<Long>) {
        viewModelScope.launch { reorderTracksUseCase(playlistId, orderedMediaIds) }
    }

    fun play(startIndex: Int = 0) {
        viewModelScope.launch { playPlaylistUseCase(playlistId, startIndex) }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
