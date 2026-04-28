package dev.maxxximgb.genesis.data.local.dao

import androidx.room.Embedded
import dev.maxxximgb.genesis.data.local.entity.PlaylistEntity

data class PlaylistWithTrackCount(
    @Embedded val playlist: PlaylistEntity,
    val trackCount: Int,
)
