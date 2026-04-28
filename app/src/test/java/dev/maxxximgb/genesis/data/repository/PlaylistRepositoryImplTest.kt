package dev.maxxximgb.genesis.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.maxxximgb.genesis.data.local.AppDatabase
import dev.maxxximgb.genesis.domain.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PlaylistRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = PlaylistRepositoryImpl(
            db = db,
            playlistDao = db.playlistDao(),
            trackDao = db.trackDao(),
            playlistTrackDao = db.playlistTrackDao(),
        )
    }

    @After
    fun tearDown() = db.close()

    private fun track(id: Long) = Track(
        mediaStoreId = id,
        title = "T$id",
        artist = "A$id",
        album = "Al$id",
        albumId = id,
        durationMs = 1000L,
        contentUri = "u://$id",
        dateAdded = id,
    )

    @Test
    fun createReturnsIdAndObservePlaylistsEmits() = runTest {
        val id = repo.create("My Playlist")
        val list = repo.observePlaylists().first()
        assertEquals(1, list.size)
        assertEquals(id, list[0].id)
        assertEquals("My Playlist", list[0].name)
    }

    @Test
    fun renameUpdatesName() = runTest {
        val id = repo.create("Old")
        repo.rename(id, "New")
        assertEquals("New", repo.getPlaylistName(id))
    }

    @Test
    fun deletePlaylistAlsoCascadesJoinRows() = runTest {
        val id = repo.create("P")
        repo.addTracks(id, listOf(track(1L), track(2L)))
        repo.delete(id)
        assertNull(repo.observePlaylist(id).first())
        assertEquals(emptyList<Track>(), repo.getTracksForPlaylist(id))
    }

    @Test
    fun addTracksAppendsAtMaxPositionPlusOne() = runTest {
        val id = repo.create("P")
        repo.addTracks(id, listOf(track(1L), track(2L), track(3L)))
        repo.addTracks(id, listOf(track(4L), track(5L)))

        val tracks = repo.getTracksForPlaylist(id)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), tracks.map { it.mediaStoreId })
    }

    @Test
    fun addTracksDeduplicatesByCompositeKey() = runTest {
        val id = repo.create("P")
        repo.addTracks(id, listOf(track(1L), track(2L)))
        // Adding the same tracks again — the @Insert IGNORE keeps existing rows;
        // those positions stay at 0 and 1.
        repo.addTracks(id, listOf(track(1L), track(2L)))

        val tracks = repo.getTracksForPlaylist(id)
        assertEquals(listOf(1L, 2L), tracks.map { it.mediaStoreId })
    }

    @Test
    fun removeTracksClearsOnlyTargetedRows() = runTest {
        val id = repo.create("P")
        repo.addTracks(id, listOf(track(1L), track(2L), track(3L)))
        repo.removeTracks(id, listOf(2L))

        val ids = repo.getTracksForPlaylist(id).map { it.mediaStoreId }
        assertEquals(listOf(1L, 3L), ids)
    }

    @Test
    fun reorderPropagatesNewOrder() = runTest {
        val id = repo.create("P")
        repo.addTracks(id, (1L..5L).map { track(it) })
        repo.reorder(id, listOf(5L, 3L, 1L, 4L, 2L))

        val ids = repo.getTracksForPlaylist(id).map { it.mediaStoreId }
        assertEquals(listOf(5L, 3L, 1L, 4L, 2L), ids)
    }

    @Test
    fun observePlaylistTracksReflectsChanges() = runTest {
        val id = repo.create("P")
        assertEquals(emptyList<Track>(), repo.observePlaylistTracks(id).first())

        repo.addTracks(id, listOf(track(1L), track(2L)))
        val withTracks = repo.observePlaylistTracks(id).first()
        assertEquals(listOf(1L, 2L), withTracks.map { it.mediaStoreId })
    }

    @Test
    fun observePlaylistEmitsNullForUnknownId() = runTest {
        assertNull(repo.observePlaylist(999L).first())
    }

    @Test
    fun observePlaylistFindsExistingPlaylist() = runTest {
        val id = repo.create("Found")
        val playlist = repo.observePlaylist(id).first()
        assertNotNull(playlist)
        assertEquals("Found", playlist!!.name)
    }
}
