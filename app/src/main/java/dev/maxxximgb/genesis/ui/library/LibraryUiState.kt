package dev.maxxximgb.genesis.ui.library

import dev.maxxximgb.genesis.domain.model.SortOrder

enum class LibraryBrowseTab { TRACKS, ALBUMS, PLAYLISTS, AUDIOBOOKS }

enum class AlbumsLayout { LIST, GRID }

data class LibraryUiState(
    val searchQuery: String = "",
    val sort: SortOrder = SortOrder.DATE_ADDED_DESC,
    val selectedIds: Set<Long> = emptySet(),
    val selectedPlaylistIds: Set<Long> = emptySet(),
    val selectedAlbumIds: Set<Long> = emptySet(),
    val currentMediaStoreId: Long? = null,
    val browseTab: LibraryBrowseTab = LibraryBrowseTab.TRACKS,
    val recentSearches: List<String> = emptyList(),
    val albumsLayout: AlbumsLayout = AlbumsLayout.LIST,
) {
    val selectionMode: Boolean get() = selectedIds.isNotEmpty()
    val playlistSelectionMode: Boolean get() = selectedPlaylistIds.isNotEmpty()
    val albumSelectionMode: Boolean get() = selectedAlbumIds.isNotEmpty()
    /** Any kind of selection — for chrome that hides regardless (chips, mini player). */
    val anySelectionMode: Boolean
        get() = selectionMode || playlistSelectionMode || albumSelectionMode
}
