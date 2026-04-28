package dev.maxxximgb.genesis.domain.model

data class Track(
    val mediaStoreId: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val albumId: Long?,
    val durationMs: Long,
    val contentUri: String,
    val dateAdded: Long,
)
