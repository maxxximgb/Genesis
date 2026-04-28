package com.maximg.player.data

import androidx.room.Entity

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "mediaStoreId"]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val mediaStoreId: Long,
    val position: Int
)
