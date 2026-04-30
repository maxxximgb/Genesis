package dev.maxxximgb.genesis.data.media

sealed class LibraryFilter {
    data object None : LibraryFilter()
    data class ByAlbum(val albumId: Long) : LibraryFilter()
    data class ByArtist(val artistId: Long) : LibraryFilter()
    data class ByFolder(val bucketId: Long) : LibraryFilter()
}
