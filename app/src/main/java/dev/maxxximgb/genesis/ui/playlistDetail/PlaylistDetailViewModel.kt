package dev.maxxximgb.genesis.ui.playlistDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.data.playback.PlaybackStateStore
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.usecase.playback.PlayPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.AddTracksToPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistDetailUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.RemoveTracksFromPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ReorderPlaylistTracksUseCase
import dev.maxxximgb.genesis.ui.library.SelectionStateHolder
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
    private val userPreferences: UserPreferencesStore,
    private val selectionStateHolder: SelectionStateHolder,
) : ViewModel() {

    val playlistId: Long = checkNotNull(savedStateHandle["playlistId"]) {
        "PlaylistDetailViewModel requires `playlistId` in SavedStateHandle"
    }

    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val reorderMode = MutableStateFlow(false)

    private val detailFlow = observePlaylistDetail(playlistId)
    private val currentMediaStoreIdFlow =
        playbackStateStore.flow.map { it.currentMediaStoreId }.distinctUntilChanged()

    val uiState: StateFlow<PlaylistDetailUiState> = combine(
        detailFlow,
        selectedIds,
        currentMediaStoreIdFlow,
        reorderMode,
    ) { detail, selected, currentId, reorder ->
        when {
            detail == null -> PlaylistDetailUiState.NotFound
            else -> PlaylistDetailUiState.Content(
                detail = detail,
                selectedIds = selected,
                currentMediaStoreId = currentId,
                reorderMode = reorder && selected.isEmpty(),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = PlaylistDetailUiState.Loading,
    )

    fun setReorderMode(enabled: Boolean) {
        reorderMode.value = enabled
        if (enabled) clearSelection()
    }

    fun toggleSelection(mediaStoreId: Long) {
        selectedIds.update { current ->
            if (mediaStoreId in current) current - mediaStoreId else current + mediaStoreId
        }
        // Mini Now Playing bar reads SelectionStateHolder to slide itself away while the
        // user is selecting tracks anywhere in the app — same global signal the library uses.
        selectionStateHolder.set(selectedIds.value.isNotEmpty())
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
        selectionStateHolder.set(false)
    }

    override fun onCleared() {
        // Leaving the screen by back-press while a selection was active would otherwise
        // strand the holder in `active=true` and keep the mini bar permanently hidden.
        selectionStateHolder.set(false)
        super.onCleared()
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

    /**
     * Drag-handle reorder: take the track at [fromIndex] and insert it at [toIndex].
     * Different shape than [moveSelected] (which is ±1 nudge for the multi-select toolbar):
     * here we already know the absolute destination from the drop position.
     */
    fun moveTrack(fromIndex: Int, toIndex: Int) {
        val state = uiState.value as? PlaylistDetailUiState.Content ?: return
        val tracks = state.detail.tracks
        if (fromIndex !in tracks.indices || toIndex !in tracks.indices) return
        if (fromIndex == toIndex) return
        val newOrder = tracks.map { it.mediaStoreId }.toMutableList().apply {
            val item = removeAt(fromIndex)
            add(toIndex, item)
        }
        viewModelScope.launch { reorderTracksUseCase(playlistId, newOrder) }
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

    /**
     * Explicit row tap from PlaylistDetail — pass the index as `explicitStartIndex` so the
     * use case starts from the chosen track, not from the saved bookmark.
     */
    fun play(startIndex: Int) {
        viewModelScope.launch { playPlaylistUseCase(playlistId, explicitStartIndex = startIndex) }
    }

    /** Mark every track of this playlist as an audiobook (idempotent override). */
    suspend fun markAllAsAudiobook(): Int {
        val state = uiState.value as? PlaylistDetailUiState.Content ?: return 0
        val ids = state.detail.tracks.map { it.mediaStoreId }
        if (ids.isEmpty()) return 0
        userPreferences.addAudiobookOverrides(ids)
        return ids.size
    }

    /** "Resume" entry-point — let the use case consult the bookmark. */
    fun resume() {
        viewModelScope.launch { playPlaylistUseCase(playlistId) }
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
