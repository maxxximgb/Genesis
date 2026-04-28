package dev.maxxximgb.genesis.ui.library

import dev.maxxximgb.genesis.domain.model.SortOrder

data class LibraryUiState(
    val searchQuery: String = "",
    val sort: SortOrder = SortOrder.DATE_ADDED_DESC,
)
