package com.maximg.player.data

import android.net.Uri
import com.maximg.player.media.MediaTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class PlaylistRepositoryTest {
    @Test
    fun addTracksToPlaylist_usesMaxPositionOffsetAndUpsertsTracks() = runBlocking {
        val dao = FakePlaylistDao().apply { maxPosition = 4 }
        val repository = PlaylistRepository(dao)

        repository.addTracksToPlaylist(
            playlistId = 7L,
            tracks = listOf(mediaTrack(10L), mediaTrack(20L))
        )

        assertEquals(listOf(10L, 20L), dao.upsertedTracks.map { it.mediaStoreId })
        assertEquals(
            listOf(
                PlaylistTrackEntity(playlistId = 7L, mediaStoreId = 10L, position = 5),
                PlaylistTrackEntity(playlistId = 7L, mediaStoreId = 20L, position = 6)
            ),
            dao.insertedPlaylistTracks
        )
    }

    @Test
    fun addTracksToPlaylist_startsPositionsFromZeroWhenPlaylistEmpty() = runBlocking {
        val dao = FakePlaylistDao().apply { maxPosition = null }
        val repository = PlaylistRepository(dao)

        repository.addTracksToPlaylist(
            playlistId = 3L,
            tracks = listOf(mediaTrack(42L))
        )

        assertEquals(
            listOf(PlaylistTrackEntity(playlistId = 3L, mediaStoreId = 42L, position = 0)),
            dao.insertedPlaylistTracks
        )
    }

    @Test
    fun addTracksToPlaylist_withEmptyTracks_doesNothing() = runBlocking {
        val dao = FakePlaylistDao()
        val repository = PlaylistRepository(dao)

        repository.addTracksToPlaylist(playlistId = 1L, tracks = emptyList())

        assertTrue(dao.upsertedTracks.isEmpty())
        assertTrue(dao.insertedPlaylistTracks.isEmpty())
    }

    @Test
    fun removeTracksFromPlaylist_withEmptyList_doesNothing() = runBlocking {
        val dao = FakePlaylistDao()
        val repository = PlaylistRepository(dao)

        repository.removeTracksFromPlaylist(playlistId = 9L, mediaIds = emptyList())

        assertTrue(dao.batchRemoveRequests.isEmpty())
    }

    @Test
    fun removeTrackFromPlaylist_delegatesAsSingleBatchRequest() = runBlocking {
        val dao = FakePlaylistDao()
        val repository = PlaylistRepository(dao)

        repository.removeTrackFromPlaylist(playlistId = 2L, mediaStoreId = 99L)

        assertEquals(listOf(2L to listOf(99L)), dao.batchRemoveRequests)
    }

    @Test
    fun deletePlaylist_deletesTracksBeforePlaylistEntity() = runBlocking {
        val dao = FakePlaylistDao()
        val repository = PlaylistRepository(dao)

        repository.deletePlaylist(5L)

        assertEquals(listOf("tracks:5", "playlist:5"), dao.deleteOrder)
    }

    @Test
    fun renamePlaylist_updatesWhenPlaylistExists() = runBlocking {
        val dao = FakePlaylistDao().apply {
            playlists[1L] = PlaylistEntity(id = 1L, name = "Old", createdAt = 123L)
        }
        val repository = PlaylistRepository(dao)

        repository.renamePlaylist(id = 1L, newName = "New")

        assertEquals("New", dao.playlists[1L]?.name)
        assertEquals(123L, dao.playlists[1L]?.createdAt)
    }

    @Test
    fun renamePlaylist_whenPlaylistMissing_doesNothing() = runBlocking {
        val dao = FakePlaylistDao()
        val repository = PlaylistRepository(dao)

        repository.renamePlaylist(id = 404L, newName = "Does not matter")

        assertNull(dao.lastUpdatedPlaylist)
    }

    @Test
    fun reorderTracks_delegatesToDao() = runBlocking {
        val dao = FakePlaylistDao()
        val repository = PlaylistRepository(dao)

        repository.reorderTracks(playlistId = 8L, orderedMediaIds = listOf(3L, 1L, 2L))

        assertEquals(listOf(8L to listOf(3L, 1L, 2L)), dao.reorderRequests)
    }

    private fun mediaTrack(id: Long): MediaTrack {
        val uri = Mockito.mock(Uri::class.java)
        Mockito.`when`(uri.toString()).thenReturn("content://media/$id")

        return MediaTrack(
            mediaStoreId = id,
            title = "Track $id",
            artist = "Artist $id",
            album = "Album $id",
            durationMs = 180_000L,
            contentUri = uri
        )
    }

    private class FakePlaylistDao : PlaylistDao {
        val playlists = mutableMapOf<Long, PlaylistEntity>()
        var maxPosition: Int? = null

        val upsertedTracks = mutableListOf<TrackEntity>()
        val insertedPlaylistTracks = mutableListOf<PlaylistTrackEntity>()
        val batchRemoveRequests = mutableListOf<Pair<Long, List<Long>>>()
        val deleteOrder = mutableListOf<String>()
        val reorderRequests = mutableListOf<Pair<Long, List<Long>>>()

        var nextPlaylistId = 1L
        var lastUpdatedPlaylist: PlaylistEntity? = null

        override fun observePlaylists(): Flow<List<PlaylistEntity>> = flowOf(playlists.values.toList())

        override fun observePlaylist(id: Long): Flow<PlaylistEntity?> = flowOf(playlists[id])

        override suspend fun getPlaylist(id: Long): PlaylistEntity? = playlists[id]

        override suspend fun getPlaylistName(id: Long): String? = playlists[id]?.name

        override suspend fun insertPlaylist(entity: PlaylistEntity): Long {
            val assignedId = if (entity.id == 0L) nextPlaylistId++ else entity.id
            playlists[assignedId] = entity.copy(id = assignedId)
            return assignedId
        }

        override suspend fun updatePlaylist(entity: PlaylistEntity) {
            playlists[entity.id] = entity
            lastUpdatedPlaylist = entity
        }

        override suspend fun deletePlaylistById(id: Long) {
            deleteOrder += "playlist:$id"
            playlists.remove(id)
        }

        override suspend fun upsertTrack(entity: TrackEntity) {
            upsertedTracks += entity
        }

        override suspend fun insertPlaylistTrack(entity: PlaylistTrackEntity): Long {
            insertedPlaylistTracks += entity
            return 1L
        }

        override suspend fun insertPlaylistTracks(entities: List<PlaylistTrackEntity>) {
            insertedPlaylistTracks += entities
        }

        override suspend fun deleteTracksForPlaylist(playlistId: Long) {
            deleteOrder += "tracks:$playlistId"
        }

        override suspend fun removeTrackFromPlaylist(playlistId: Long, mediaStoreId: Long) {
            batchRemoveRequests += playlistId to listOf(mediaStoreId)
        }

        override suspend fun removeTracksFromPlaylist(playlistId: Long, mediaIds: List<Long>) {
            batchRemoveRequests += playlistId to mediaIds
        }

        override suspend fun getMaxPosition(playlistId: Long): Int? = maxPosition

        override fun observePlaylistTrackItems(playlistId: Long): Flow<List<PlaylistTrackItem>> = flowOf(emptyList())

        override suspend fun getPlaylistTrackItems(playlistId: Long): List<PlaylistTrackItem> = emptyList()

        override suspend fun updateTrackPosition(playlistId: Long, mediaStoreId: Long, position: Int) {
            Unit
        }

        override suspend fun reorderTracks(playlistId: Long, orderedMediaIds: List<Long>) {
            reorderRequests += playlistId to orderedMediaIds
        }
    }
}
