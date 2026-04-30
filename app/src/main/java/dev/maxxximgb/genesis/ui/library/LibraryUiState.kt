package dev.maxxximgb.genesis.ui.library

import dev.maxxximgb.genesis.domain.model.SortOrder

enum class LibraryBrowseTab { TRACKS, ALBUMS, ARTISTS, FOLDERS }

enum class AlbumsLayout { LIST, GRID }

data class LibraryUiState(
    val searchQuery: String = "",
    val sort: SortOrder = SortOrder.DATE_ADDED_DESC,
    val selectedIds: Set<Long> = emptySet(),
    val currentMediaStoreId: Long? = null,
    val browseTab: LibraryBrowseTab = LibraryBrowseTab.TRACKS,
    val recentSearches: List<String> = emptyList(),
    val albumsLayout: AlbumsLayout = AlbumsLayout.LIST,
) {
    val selectionMode: Boolean get() = selectedIds.isNotEmpty()
}
