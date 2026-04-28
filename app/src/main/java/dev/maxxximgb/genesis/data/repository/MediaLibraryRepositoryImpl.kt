package dev.maxxximgb.genesis.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import dev.maxxximgb.genesis.data.media.MediaStorePagingSource
import dev.maxxximgb.genesis.data.media.MediaStoreSource
import dev.maxxximgb.genesis.domain.model.Album
import dev.maxxximgb.genesis.domain.model.Artist
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaLibraryRepositoryImpl @Inject constructor(
    private val source: MediaStoreSource,
) : MediaLibraryRepository {

    override fun pagedLibrary(sort: SortOrder, query: String): Pager<Int, Track> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { MediaStorePagingSource(source, sort, query) },
    )

    override suspend fun findById(mediaStoreId: Long): Track? = source.findById(mediaStoreId)

    // Album / Artist views land in 2.2 — stable signature reserved.
    override fun observeAlbums(): Flow<List<Album>> = flowOf(emptyList())

    override fun observeArtists(): Flow<List<Artist>> = flowOf(emptyList())

    private companion object {
        const val PAGE_SIZE = 50
        const val PREFETCH_DISTANCE = 10
    }
}
