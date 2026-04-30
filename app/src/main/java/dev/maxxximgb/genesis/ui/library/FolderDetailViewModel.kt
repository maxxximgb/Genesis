package dev.maxxximgb.genesis.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.domain.model.Folder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.usecase.library.ObserveFoldersUseCase
import dev.maxxximgb.genesis.domain.usecase.library.TracksInFolderUseCase
import dev.maxxximgb.genesis.ui.navigation.Routes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeFolders: ObserveFoldersUseCase,
    tracksInFolder: TracksInFolderUseCase,
) : ViewModel() {

    val bucketId: Long = checkNotNull(savedStateHandle[Routes.FolderDetail.ARG_BUCKET_ID])

    val folder: StateFlow<Folder?> = observeFolders()
        .map { list -> list.firstOrNull { it.bucketId == bucketId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = null,
        )

    val pagedTracks: Flow<PagingData<Track>> = tracksInFolder(bucketId).flow.cachedIn(viewModelScope)

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
