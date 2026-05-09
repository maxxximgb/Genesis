package dev.maxxximgb.genesis.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.data.preferences.SearchHistoryStore
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.usecase.library.SearchLibraryUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.AddTracksToPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.CreatePlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import dev.maxxximgb.genesis.ui.library.SelectionStateHolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchLibrary: SearchLibraryUseCase,
    private val userPreferences: UserPreferencesStore,
    private val searchHistoryStore: SearchHistoryStore,
    private val addTracksToPlaylist: AddTracksToPlaylistUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val selectionStateHolder: SelectionStateHolder,
    observePlaylists: ObservePlaylistsUseCase,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val selectedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    private val selectedTracksMap = MutableStateFlow<Map<Long, Track>>(emptyMap())
    val selectedIds: StateFlow<Set<Long>> = selectedTrackIds.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = observePlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    val audiobookOverrides: StateFlow<Set<Long>> = userPreferences.observeAudiobookOverrides()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptySet(),
        )

    val recentSearches: StateFlow<List<String>> = searchHistoryStore.observeRecent()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val pagedResults: Flow<PagingData<Track>> = combine(
        userPreferences.observeLibrarySort(),
        _query.debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MS },
    ) { sort, query -> sort to query }
        .flatMapLatest { (sort, query) -> searchLibrary(sort, query).flow }
        .cachedIn(viewModelScope)

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onSubmit() {
        val q = _query.value.trim()
        if (q.isEmpty()) return
        viewModelScope.launch { searchHistoryStore.add(q) }
    }

    fun onRecentClick(recent: String) {
        _query.value = recent
        viewModelScope.launch { searchHistoryStore.add(recent) }
    }

    fun onRecentRemove(recent: String) {
        viewModelScope.launch { searchHistoryStore.remove(recent) }
    }

    fun toggleSelection(track: Track) {
        selectedTrackIds.update { current ->
            if (track.mediaStoreId in current) current - track.mediaStoreId
            else current + track.mediaStoreId
        }
        selectedTracksMap.update { current ->
            if (track.mediaStoreId in selectedTrackIds.value) {
                current + (track.mediaStoreId to track)
            } else {
                current
            }
        }
        publishSelectionState()
    }

    fun clearSelection() {
        selectedTrackIds.value = emptySet()
        selectedTracksMap.value = emptyMap()
        publishSelectionState()
    }

    fun toggleAudiobookOverride(mediaStoreId: Long) {
        viewModelScope.launch { userPreferences.toggleAudiobookOverride(mediaStoreId) }
    }

    suspend fun markSelectedAsAudiobooks(): Int {
        val ids = selectedTrackIds.value
        if (ids.isEmpty()) return 0
        userPreferences.addAudiobookOverrides(ids)
        val count = ids.size
        clearSelection()
        return count
    }

    suspend fun addSingleToPlaylist(track: Track, playlistId: Long) {
        addTracksToPlaylist(playlistId, listOf(track))
    }

    suspend fun addSelectedToPlaylist(playlistId: Long): Int {
        val tracks = selectedTrackIds.value.mapNotNull { selectedTracksMap.value[it] }
        if (tracks.isEmpty()) return 0
        addTracksToPlaylist(playlistId, tracks)
        clearSelection()
        return tracks.size
    }

    suspend fun createPlaylist(name: String): Long = createPlaylistUseCase(name)

    private fun publishSelectionState() {
        selectionStateHolder.set(selectedTrackIds.value.isNotEmpty())
    }

    override fun onCleared() {
        selectionStateHolder.set(false)
        super.onCleared()
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
