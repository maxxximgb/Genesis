package dev.maxxximgb.genesis.data.local

import dev.maxxximgb.genesis.data.local.entity.PlaylistEntity
import dev.maxxximgb.genesis.data.local.entity.TrackEntity
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.Track

internal fun PlaylistEntity.toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    createdAt = createdAt,
)

internal fun TrackEntity.toDomain(): Track = Track(
    mediaStoreId = mediaStoreId,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    contentUri = contentUri,
    dateAdded = dateAdded,
)

internal fun Track.toEntity(): TrackEntity = TrackEntity(
    mediaStoreId = mediaStoreId,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    contentUri = contentUri,
    dateAdded = dateAdded,
)
