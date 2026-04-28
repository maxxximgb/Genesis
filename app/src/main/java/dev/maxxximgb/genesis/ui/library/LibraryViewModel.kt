package dev.maxxximgb.genesis.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.usecase.library.SearchLibraryUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.AddTracksToPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.CreatePlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
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
class LibraryViewModel @Inject constructor(
    private val searchLibrary: SearchLibraryUseCase,
    private val userPreferences: UserPreferencesStore,
    private val addTracksToPlaylist: AddTracksToPlaylistUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    observePlaylists: ObservePlaylistsUseCase,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    private val selectedTracksMap = MutableStateFlow<Map<Long, Track>>(emptyMap())

    val uiState: StateFlow<LibraryUiState> = combine(
        searchQuery.asStateFlow(),
        userPreferences.observeLibrarySort(),
        selectedTrackIds.asStateFlow(),
    ) { query, sort, selected ->
        LibraryUiState(searchQuery = query, sort = sort, selectedIds = selected)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = LibraryUiState(),
    )

    val playlists: StateFlow<List<Playlist>> = observePlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val pagedTracks: Flow<PagingData<Track>> = combine(
        userPreferences.observeLibrarySort(),
        searchQuery.debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MS },
    ) { sort, query -> sort to query }
        .flatMapLatest { (sort, query) -> searchLibrary(sort, query).flow }
        .cachedIn(viewModelScope)

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onSortChange(sort: SortOrder) {
        viewModelScope.launch { userPreferences.setLibrarySort(sort) }
    }

    fun toggleSelection(track: Track) {
        selectedTrackIds.update { current ->
            if (track.mediaStoreId in current) current - track.mediaStoreId
            else current + track.mediaStoreId
        }
        selectedTracksMap.update { it + (track.mediaStoreId to track) }
    }

    fun clearSelection() {
        selectedTrackIds.value = emptySet()
        selectedTracksMap.value = emptyMap()
    }

    suspend fun addSingleToPlaylist(track: Track, playlistId: Long) {
        addTracksToPlaylist(playlistId, listOf(track))
    }

    suspend fun addSelectedToPlaylist(playlistId: Long): Int {
        val ids = selectedTrackIds.value
        val tracks = ids.mapNotNull { selectedTracksMap.value[it] }
        if (tracks.isEmpty()) return 0
        addTracksToPlaylist(playlistId, tracks)
        clearSelection()
        return tracks.size
    }

    suspend fun createPlaylist(name: String): Long = createPlaylistUseCase(name)

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
