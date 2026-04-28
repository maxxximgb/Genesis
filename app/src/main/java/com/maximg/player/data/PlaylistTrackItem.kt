package com.maximg.player.data

data class PlaylistTrackItem(
    val mediaStoreId: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val contentUri: String,
    val position: Int
)
