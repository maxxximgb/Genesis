package dev.maxxximgb.genesis.ui.playlistDetail

import dev.maxxximgb.genesis.domain.model.PlaylistDetail

sealed interface PlaylistDetailUiState {
    data object Loading : PlaylistDetailUiState

    data class Content(
        val detail: PlaylistDetail,
        val selectedIds: Set<Long> = emptySet(),
    ) : PlaylistDetailUiState {
        val selectionMode: Boolean get() = selectedIds.isNotEmpty()
    }

    data object NotFound : PlaylistDetailUiState
}
