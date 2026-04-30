package dev.maxxximgb.genesis.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import dev.maxxximgb.genesis.data.media.LibraryFilter
import dev.maxxximgb.genesis.data.media.MediaStorePagingSource
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaLibraryRepositoryImpl @Inject constructor(
    private val source: MediaStoreSource,
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

    override suspend fun findById(mediaStoreId: Long): Track? = source.findById(mediaStoreId)

    override fun observeAlbums(): Flow<List<Album>> = flow { emit(source.queryAlbums()) }
        .flowOn(Dispatchers.IO)

    override fun observeArtists(): Flow<List<Artist>> = flow { emit(source.queryArtists()) }
        .flowOn(Dispatchers.IO)

    override fun observeFolders(): Flow<List<Folder>> = flow { emit(source.queryFolders()) }
        .flowOn(Dispatchers.IO)

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
