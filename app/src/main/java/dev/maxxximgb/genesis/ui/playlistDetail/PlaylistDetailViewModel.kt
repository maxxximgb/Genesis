package dev.maxxximgb.genesis.ui.playlistDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.data.playback.PlaybackStateStore
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.usecase.playback.PlayPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.AddTracksToPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistDetailUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.RemoveTracksFromPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ReorderPlaylistTracksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePlaylistDetail: ObservePlaylistDetailUseCase,
    playbackStateStore: PlaybackStateStore,
    private val addTracksUseCase: AddTracksToPlaylistUseCase,
    private val removeTracksUseCase: RemoveTracksFromPlaylistUseCase,
    private val reorderTracksUseCase: ReorderPlaylistTracksUseCase,
    private val playPlaylistUseCase: PlayPlaylistUseCase,
) : ViewModel() {

    val playlistId: Long = checkNotNull(savedStateHandle["playlistId"]) {
        "PlaylistDetailViewModel requires `playlistId` in SavedStateHandle"
    }

    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    private val detailFlow = observePlaylistDetail(playlistId)
    private val currentMediaStoreIdFlow =
        playbackStateStore.flow.map { it.currentMediaStoreId }.distinctUntilChanged()

    val uiState: StateFlow<PlaylistDetailUiState> = combine(
        detailFlow,
        selectedIds,
        currentMediaStoreIdFlow,
    ) { detail, selected, currentId ->
        when {
            detail == null -> PlaylistDetailUiState.NotFound
            else -> PlaylistDetailUiState.Content(
                detail = detail,
                selectedIds = selected,
                currentMediaStoreId = currentId,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = PlaylistDetailUiState.Loading,
    )

    fun toggleSelection(mediaStoreId: Long) {
        selectedIds.update { current ->
            if (mediaStoreId in current) current - mediaStoreId else current + mediaStoreId
        }
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun addTracks(tracks: List<Track>) {
        viewModelScope.launch { addTracksUseCase(playlistId, tracks) }
    }

    fun removeSelected(): Int {
        val ids = selectedIds.value
        if (ids.isEmpty()) return 0
        viewModelScope.launch {
            removeTracksUseCase(playlistId, ids.toList())
        }
        clearSelection()
        return ids.size
    }

    fun removeOne(mediaStoreId: Long) {
        viewModelScope.launch {
            removeTracksUseCase(playlistId, listOf(mediaStoreId))
        }
    }

    fun moveSelected(direction: Int) {
        val state = uiState.value as? PlaylistDetailUiState.Content ?: return
        val tracks = state.detail.tracks
        val ids = state.selectedIds
        if (ids.isEmpty() || tracks.isEmpty()) return

        val currentOrder = tracks.map { it.mediaStoreId }
        val newOrder = computeMove(currentOrder, ids, direction)
        if (newOrder == currentOrder) return

        viewModelScope.launch { reorderTracksUseCase(playlistId, newOrder) }
    }

    fun play(startIndex: Int = 0) {
        viewModelScope.launch { playPlaylistUseCase(playlistId, startIndex) }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

internal fun computeMove(
    currentOrder: List<Long>,
    selected: Set<Long>,
    direction: Int,
): List<Long> {
    if (direction == 0 || selected.isEmpty()) return currentOrder
    val result = currentOrder.toMutableList()
    val indices = result.withIndex()
        .filter { (_, id) -> id in selected }
        .map { it.index }
        .let { if (direction > 0) it.sortedDescending() else it.sorted() }
    for (idx in indices) {
        val target = idx + direction
        if (target < 0 || target >= result.size) continue
        if (result[target] in selected) continue
        val tmp = result[idx]
        result[idx] = result[target]
        result[target] = tmp
    }
    return result
}
