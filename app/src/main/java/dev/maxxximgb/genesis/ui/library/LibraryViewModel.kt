package dev.maxxximgb.genesis.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.usecase.library.SearchLibraryUseCase
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val searchLibrary: SearchLibraryUseCase,
    private val userPreferences: UserPreferencesStore,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<LibraryUiState> = combine(
        searchQuery.asStateFlow(),
        userPreferences.observeLibrarySort(),
    ) { query, sort -> LibraryUiState(searchQuery = query, sort = sort) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = LibraryUiState(),
        )

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val pagedTracks: Flow<PagingData<Track>> = combine(
        userPreferences.observeLibrarySort(),
        searchQuery.debounce { query -> if (query.isEmpty()) 0L else SEARCH_DEBOUNCE_MS },
    ) { sort, query -> sort to query }
        .flatMapLatest { (sort, query) -> searchLibrary(sort, query).flow }
        .cachedIn(viewModelScope)

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onSortChange(sort: SortOrder) {
        viewModelScope.launch { userPreferences.setLibrarySort(sort) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
