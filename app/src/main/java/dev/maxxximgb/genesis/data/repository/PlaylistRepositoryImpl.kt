package dev.maxxximgb.genesis.data.repository

import androidx.room.withTransaction
import dev.maxxximgb.genesis.data.local.AppDatabase
import dev.maxxximgb.genesis.data.local.dao.PlaylistDao
import dev.maxxximgb.genesis.data.local.dao.PlaylistTrackDao
import dev.maxxximgb.genesis.data.local.dao.TrackDao
import dev.maxxximgb.genesis.data.local.entity.PlaylistEntity
import dev.maxxximgb.genesis.data.local.entity.PlaylistTrackEntity
import dev.maxxximgb.genesis.data.local.toDomain
import dev.maxxximgb.genesis.data.local.toEntity
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val playlistDao: PlaylistDao,
    private val trackDao: TrackDao,
    private val playlistTrackDao: PlaylistTrackDao,
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> =
        playlistDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observePlaylistsWithCounts():
        Flow<List<dev.maxxximgb.genesis.domain.model.PlaylistSummary>> =
        playlistDao.observeAllWithCounts().map { rows ->
            rows.map { row ->
                dev.maxxximgb.genesis.domain.model.PlaylistSummary(
                    playlist = row.playlist.toDomain(),
                    trackCount = row.trackCount,
                )
            }
        }

    override fun observePlaylist(id: Long): Flow<Playlist?> =
        playlistDao.observeAll()
            .map { rows -> rows.firstOrNull { it.id == id }?.toDomain() }

    override fun observePlaylistTracks(playlistId: Long): Flow<List<Track>> =
        playlistTrackDao.observeTracksForPlaylist(playlistId)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getPlaylistName(id: Long): String? =
        playlistDao.findById(id)?.name

    override suspend fun getTracksForPlaylist(playlistId: Long): List<Track> =
        playlistTrackDao.getTracksForPlaylist(playlistId).map { it.toDomain() }

    override suspend fun create(name: String): Long =
        playlistDao.insert(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))

    override suspend fun rename(id: Long, name: String) {
        playlistDao.rename(id, name)
    }

    override suspend fun delete(id: Long) {
        playlistDao.delete(id)
    }

    override suspend fun addTracks(playlistId: Long, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        db.withTransaction {
            trackDao.upsertAll(tracks.map { it.toEntity() })
            val basePosition = (playlistTrackDao.maxPositionForPlaylist(playlistId) ?: -1) + 1
            val rows = tracks.mapIndexed { idx, track ->
                PlaylistTrackEntity(playlistId, track.mediaStoreId, basePosition + idx)
            }
            playlistTrackDao.insertIgnore(rows)
        }
    }

    override suspend fun removeTracks(playlistId: Long, mediaStoreIds: List<Long>) {
        if (mediaStoreIds.isEmpty()) return
        playlistTrackDao.deleteAll(playlistId, mediaStoreIds)
    }

    override suspend fun reorder(playlistId: Long, orderedMediaIds: List<Long>) {
        playlistTrackDao.reorder(playlistId, orderedMediaIds)
    }
}
