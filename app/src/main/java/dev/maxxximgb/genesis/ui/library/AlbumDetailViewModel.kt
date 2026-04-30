package dev.maxxximgb.genesis.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.domain.model.Album
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.usecase.library.ObserveAlbumsUseCase
import dev.maxxximgb.genesis.domain.usecase.library.TracksByAlbumUseCase
import dev.maxxximgb.genesis.ui.navigation.Routes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeAlbums: ObserveAlbumsUseCase,
    tracksByAlbum: TracksByAlbumUseCase,
) : ViewModel() {

    val albumId: Long = checkNotNull(savedStateHandle[Routes.AlbumDetail.ARG_ALBUM_ID])

    val album: StateFlow<Album?> = observeAlbums()
        .map { list -> list.firstOrNull { it.id == albumId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = null,
        )

    val pagedTracks: Flow<PagingData<Track>> = tracksByAlbum(albumId).flow.cachedIn(viewModelScope)

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
