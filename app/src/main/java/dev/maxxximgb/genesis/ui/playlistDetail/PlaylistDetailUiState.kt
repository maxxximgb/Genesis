package dev.maxxximgb.genesis.ui.playlistDetail

import dev.maxxximgb.genesis.domain.model.PlaylistDetail

sealed interface PlaylistDetailUiState {
    data object Loading : PlaylistDetailUiState

    data class Content(
        val detail: PlaylistDetail,
        val selectedIds: Set<Long> = emptySet(),
        val currentMediaStoreId: Long? = null,
        val reorderMode: Boolean = false,
    ) : PlaylistDetailUiState {
        val selectionMode: Boolean get() = selectedIds.isNotEmpty()
    }

    data object NotFound : PlaylistDetailUiState
}
