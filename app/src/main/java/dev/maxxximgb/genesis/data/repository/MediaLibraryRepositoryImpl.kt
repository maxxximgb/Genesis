package dev.maxxximgb.genesis.data.repository

import android.app.PendingIntent
import androidx.paging.Pager
import androidx.paging.PagingConfig
import dev.maxxximgb.genesis.data.local.dao.PlaylistTrackDao
import dev.maxxximgb.genesis.data.local.dao.TrackDao
import dev.maxxximgb.genesis.data.media.LibraryFilter
import dev.maxxximgb.genesis.data.media.MediaStorePagingSource
import dev.maxxximgb.genesis.data.media.MediaStoreRefreshSignal
import dev.maxxximgb.genesis.data.media.MediaStoreSource
import dev.maxxximgb.genesis.domain.model.Album
import dev.maxxximgb.genesis.domain.model.Artist
import dev.maxxximgb.genesis.domain.model.Folder
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaLibraryRepositoryImpl @Inject constructor(
    private val source: MediaStoreSource,
    private val trackDao: TrackDao,
    private val playlistTrackDao: PlaylistTrackDao,
    private val refreshSignal: MediaStoreRefreshSignal,
) : MediaLibraryRepository {

    override fun pagedLibrary(sort: SortOrder, query: String): Pager<Int, Track> = pager(
        sort = sort,
        query = query,
        filter = LibraryFilter.None,
    )

    override fun pagedTracksByAlbum(albumId: Long, sort: SortOrder): Pager<Int, Track> = pager(
        sort = sort,
        query = "",
        filter = LibraryFilter.ByAlbum(albumId),
    )

    override fun pagedTracksByArtist(artistId: Long, sort: SortOrder): Pager<Int, Track> = pager(
        sort = sort,
        query = "",
        filter = LibraryFilter.ByArtist(artistId),
    )

    override fun pagedTracksInFolder(bucketId: Long, sort: SortOrder): Pager<Int, Track> = pager(
        sort = sort,
        query = "",
        filter = LibraryFilter.ByFolder(bucketId),
    )

    override fun pagedAudiobooks(sort: SortOrder, overrideIds: List<Long>): Pager<Int, Track> = pager(
        sort = sort,
        query = "",
        filter = LibraryFilter.Audiobooks(overrideIds),
    )

    override suspend fun getAudiobookTracks(sort: SortOrder, overrideIds: List<Long>): List<Track> =
        materialiseAll(sort, "", LibraryFilter.Audiobooks(overrideIds))

    override suspend fun getLibraryTracks(sort: SortOrder, query: String): List<Track> =
        materialiseAll(sort, query, LibraryFilter.None)

    override suspend fun getTracksByAlbum(albumId: Long, sort: SortOrder): List<Track> =
        materialiseAll(sort, "", LibraryFilter.ByAlbum(albumId))

    private suspend fun materialiseAll(
        sort: SortOrder,
        query: String,
        filter: LibraryFilter,
    ): List<Track> = withContext(Dispatchers.IO) {
        source.queryTracks(
            sort = sort,
            query = query,
            limit = Int.MAX_VALUE,
            offset = 0,
            filter = filter,
        )
    }

    override suspend fun findById(mediaStoreId: Long): Track? =
        withContext(Dispatchers.IO) { source.findById(mediaStoreId) }

    override suspend fun getTracksByIds(ids: List<Long>): List<Track> {
        if (ids.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            val byId = source.queryByIds(ids).associateBy { it.mediaStoreId }
            ids.mapNotNull { byId[it] }
        }
    }

    override fun observeAlbums(): Flow<List<Album>> = flow { emit(source.queryAlbums()) }
        .flowOn(Dispatchers.IO)

    override fun observeArtists(): Flow<List<Artist>> = flow { emit(source.queryArtists()) }
        .flowOn(Dispatchers.IO)

    override fun observeFolders(): Flow<List<Folder>> = flow { emit(source.queryFolders()) }
        .flowOn(Dispatchers.IO)

    override fun buildDeleteRequest(mediaStoreIds: List<Long>): PendingIntent =
        source.buildDeleteRequest(mediaStoreIds)

    override fun buildWriteRequest(mediaStoreId: Long): PendingIntent =
        source.buildWriteRequest(mediaStoreId)

    override suspend fun applyRename(mediaStoreId: Long, newTitle: String): Boolean {
        return withContext(Dispatchers.IO) {
            val ok = source.applyRename(mediaStoreId, newTitle)
            if (ok) {
                // Keep the playlist/now-playing cache row consistent with the
                // user-rename override, in case the track is referenced from a
                // playlist (which renders from this Room table).
                trackDao.updateTitle(mediaStoreId, newTitle)
                // No refreshSignal here on purpose: the on-screen list applies the
                // rename as a session overlay (LibraryViewModel.pendingRenames)
                // without rebuilding the pager, so the user keeps their scroll
                // position. A pager rebuild (which would scroll to the top) is only
                // needed when the user navigates away and back — at that point the
                // override DAO supplies the value via MediaStoreSource.applyTitleOverrides.
            }
            ok
        }
    }

    override suspend fun purgeDeletedTrack(mediaStoreId: Long) {
        withContext(Dispatchers.IO) {
            // Order: drop join rows first (they reference tracks.mediaStoreId),
            // then the track row itself. Reverse order works too — there's no FK
            // cascade configured, just denormalized data — but this matches the
            // mental model of "remove the references before the thing being
            // referenced".
            playlistTrackDao.deleteByMediaStoreId(mediaStoreId)
            trackDao.delete(mediaStoreId)
            // Drop any user-rename override too — the row it targeted no longer
            // exists, and we don't want a stale override resurrecting on a future
            // mediaStoreId collision.
            source.clearTitleOverride(mediaStoreId)
            // No refreshSignal here either: the screen filters the deleted id out of
            // the visible list as a session overlay (LibraryViewModel.pendingDeletes)
            // so the row animates out under animateItemPlacement, instead of the whole
            // pager rebuilding and snapping the scroll to the top.
        }
    }

    private fun pager(sort: SortOrder, query: String, filter: LibraryFilter): Pager<Int, Track> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { MediaStorePagingSource(source, sort, query, filter) },
    )

    private companion object {
        const val PAGE_SIZE = 50
        const val PREFETCH_DISTANCE = 10
    }
}
