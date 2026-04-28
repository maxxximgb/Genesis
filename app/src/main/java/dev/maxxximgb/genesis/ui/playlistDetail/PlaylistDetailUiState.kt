package dev.maxxximgb.genesis.ui.playlistDetail

import dev.maxxximgb.genesis.domain.model.PlaylistDetail

sealed interface PlaylistDetailUiState {
    data object Loading : PlaylistDetailUiState
    data class Content(val detail: PlaylistDetail) : PlaylistDetailUiState
    data object NotFound : PlaylistDetailUiState
}
