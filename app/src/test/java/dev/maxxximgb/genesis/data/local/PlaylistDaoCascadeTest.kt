package dev.maxxximgb.genesis.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.maxxximgb.genesis.data.local.entity.PlaylistEntity
import dev.maxxximgb.genesis.data.local.entity.PlaylistTrackEntity
import dev.maxxximgb.genesis.data.local.entity.TrackEntity
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
class PlaylistDaoCascadeTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun deletingPlaylistCascadesToJoinRowsButLeavesTracks() = runTest {
        val playlistId = db.playlistDao().insert(PlaylistEntity(name = "Test", createdAt = 1L))
        val tracks = listOf(
            TrackEntity(10L, "A", "X", null, null, 1000L, "u://10", 1L),
            TrackEntity(20L, "B", "X", null, null, 1000L, "u://20", 2L),
            TrackEntity(30L, "C", "X", null, null, 1000L, "u://30", 3L),
        )
        db.trackDao().upsertAll(tracks)
        db.playlistTrackDao().insertIgnore(
            tracks.mapIndexed { idx, t -> PlaylistTrackEntity(playlistId, t.mediaStoreId, idx) }
        )

        assertEquals(3, db.playlistTrackDao().countForPlaylist(playlistId))

        db.playlistDao().delete(playlistId)

        assertEquals(0, db.playlistTrackDao().countForPlaylist(playlistId))
        assertNull(db.playlistDao().findById(playlistId))
        assertNotNull(db.trackDao().findById(10L))
        assertNotNull(db.trackDao().findById(20L))
        assertNotNull(db.trackDao().findById(30L))
    }
}
