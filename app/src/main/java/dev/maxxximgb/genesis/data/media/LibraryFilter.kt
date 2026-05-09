package dev.maxxximgb.genesis.data.media

sealed class LibraryFilter {
    data object None : LibraryFilter()
    /**
     * Audiobooks tab: native `MediaStore.IS_AUDIOBOOK = 1` files unioned with the
     * user's local override list (mediaStoreIds the user marked via the kebab menu).
     */
    data class Audiobooks(val overrideIds: List<Long> = emptyList()) : LibraryFilter()
    data class ByAlbum(val albumId: Long) : LibraryFilter()
    data class ByArtist(val artistId: Long) : LibraryFilter()
    data class ByFolder(val bucketId: Long) : LibraryFilter()
}
