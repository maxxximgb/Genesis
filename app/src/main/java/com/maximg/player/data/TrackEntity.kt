package com.maximg.player.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val mediaStoreId: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val contentUri: String
)
