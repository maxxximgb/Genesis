package dev.maxxximgb.genesis.domain.repository

import android.app.PendingIntent
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

    fun pagedAudiobooks(sort: SortOrder, overrideIds: List<Long> = emptyList()): Pager<Int, Track>

    /**
     * Snapshot of all audiobook tracks (native IS_AUDIOBOOK ∪ user overrides) ordered by [sort].
     * Used as the queue source for the audiobook widget target.
     */
    suspend fun getAudiobookTracks(sort: SortOrder, overrideIds: List<Long>): List<Track>

    /**
     * Snapshot of every library track matching [query] (or all if blank), ordered by [sort].
     * Used to build a real queue when the user taps a track in the library — the existing
     * Pager only materialises pages, so prev/next had nothing to seek to before this.
     */
    suspend fun getLibraryTracks(sort: SortOrder, query: String): List<Track>

    /**
     * Snapshot of every track in [albumId] ordered by [sort]. Used by the album detail
     * screen's "add all to playlist" action and by the albums-tab bulk action.
     */
    suspend fun getTracksByAlbum(albumId: Long, sort: SortOrder): List<Track>

    suspend fun findById(mediaStoreId: Long): Track?

    /** Returns tracks for the given mediaStoreIds, preserving the input order. */
    suspend fun getTracksByIds(ids: List<Long>): List<Track>

    fun observeAlbums(): Flow<List<Album>>

    fun observeArtists(): Flow<List<Artist>>

    fun observeFolders(): Flow<List<Folder>>

    /** Build a delete-consent PendingIntent for the given track ids. */
    fun buildDeleteRequest(mediaStoreIds: List<Long>): PendingIntent

    /** Build a write-consent PendingIntent for renaming a single track. */
    fun buildWriteRequest(mediaStoreId: Long): PendingIntent

    /**
     * Update the TITLE column. Caller must have already received a granted result
     * from [buildWriteRequest]. Returns true on success. Also propagates the change
     * to the local Room cache (`tracks.title`) so playlists / NowPlaying — which
     * read titles from Room, not MediaStore — show the new value immediately.
     */
    suspend fun applyRename(mediaStoreId: Long, newTitle: String): Boolean

    /**
     * Cleanup hook to call after the system finishes a delete consent. Removes the
     * stale Room cache entries (the join row in `playlist_tracks` AND the cached
     * row in `tracks`) so playlist UIs don't keep showing a track whose underlying
     * file is gone.
     */
    suspend fun purgeDeletedTrack(mediaStoreId: Long)
}
