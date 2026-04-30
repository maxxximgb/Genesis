package dev.maxxximgb.genesis.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.domain.model.Artist
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.usecase.library.ObserveArtistsUseCase
import dev.maxxximgb.genesis.domain.usecase.library.TracksByArtistUseCase
import dev.maxxximgb.genesis.ui.navigation.Routes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeArtists: ObserveArtistsUseCase,
    tracksByArtist: TracksByArtistUseCase,
) : ViewModel() {

    val artistId: Long = checkNotNull(savedStateHandle[Routes.ArtistDetail.ARG_ARTIST_ID])

    val artist: StateFlow<Artist?> = observeArtists()
        .map { list -> list.firstOrNull { it.id == artistId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = null,
        )

    val pagedTracks: Flow<PagingData<Track>> = tracksByArtist(artistId).flow.cachedIn(viewModelScope)

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
