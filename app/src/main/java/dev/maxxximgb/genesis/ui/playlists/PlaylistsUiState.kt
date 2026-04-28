package dev.maxxximgb.genesis.ui.playlists

import dev.maxxximgb.genesis.domain.model.PlaylistSummary

sealed interface PlaylistsUiState {
    data object Loading : PlaylistsUiState
    data class Content(val summaries: List<PlaylistSummary>) : PlaylistsUiState
    data class Error(val message: String) : PlaylistsUiState
}
