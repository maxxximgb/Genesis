package dev.maxxximgb.genesis.domain.repository

import androidx.paging.Pager
import dev.maxxximgb.genesis.domain.model.Album
import dev.maxxximgb.genesis.domain.model.Artist
import dev.maxxximgb.genesis.domain.model.Folder
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface MediaLibraryRepository {

    fun pagedLibrary(sort: SortOrder, query: String): Pager<Int, Track>

    fun pagedTracksByAlbum(albumId: Long, sort: SortOrder): Pager<Int, Track>

    fun pagedTracksByArtist(artistId: Long, sort: SortOrder): Pager<Int, Track>

    fun pagedTracksInFolder(bucketId: Long, sort: SortOrder): Pager<Int, Track>

    suspend fun findById(mediaStoreId: Long): Track?

    fun observeAlbums(): Flow<List<Album>>

    fun observeArtists(): Flow<List<Artist>>

    fun observeFolders(): Flow<List<Folder>>
}
