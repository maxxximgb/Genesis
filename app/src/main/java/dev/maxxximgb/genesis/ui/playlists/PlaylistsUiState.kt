package dev.maxxximgb.genesis.ui.playlists

import dev.maxxximgb.genesis.domain.model.Playlist

sealed interface PlaylistsUiState {
    data object Loading : PlaylistsUiState
    data class Content(val playlists: List<Playlist>) : PlaylistsUiState
    data class Error(val message: String) : PlaylistsUiState
}
