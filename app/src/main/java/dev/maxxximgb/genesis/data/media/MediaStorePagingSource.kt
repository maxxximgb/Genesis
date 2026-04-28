package dev.maxxximgb.genesis.data.media

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStorePagingSource(
    private val source: MediaStoreSource,
    private val sort: SortOrder,
    private val query: String,
) : PagingSource<Int, Track>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        val offset = params.key ?: 0
        val limit = params.loadSize
        return try {
            val tracks = withContext(Dispatchers.IO) {
                source.queryTracks(sort, query, limit, offset)
            }
            LoadResult.Page(
                data = tracks,
                prevKey = if (offset == 0) null else (offset - limit).coerceAtLeast(0),
                nextKey = if (tracks.size < limit) null else offset + limit,
            )
        } catch (t: Throwable) {
            LoadResult.Error(t)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Track>): Int? {
        val anchor = state.anchorPosition ?: return null
        val closest = state.closestPageToPosition(anchor) ?: return null
        return closest.prevKey?.plus(state.config.pageSize)
            ?: closest.nextKey?.minus(state.config.pageSize)
    }
}
