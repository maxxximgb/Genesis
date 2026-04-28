package dev.maxxximgb.genesis.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.maxxximgb.genesis.data.local.entity.PlaylistEntity
import dev.maxxximgb.genesis.data.local.entity.PlaylistTrackEntity
import dev.maxxximgb.genesis.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistTrackReorderTest {

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
    fun reorderRewritesPositionsAccordingToProvidedOrder() = runTest {
        val playlistId = db.playlistDao().insert(PlaylistEntity(name = "P", createdAt = 1L))
        val ids = listOf(10L, 20L, 30L, 40L, 50L)
        db.trackDao().upsertAll(
            ids.map { TrackEntity(it, "T$it", null, null, null, 1000L, "u://$it", it) }
        )
        db.playlistTrackDao().insertIgnore(
            ids.mapIndexed { idx, id -> PlaylistTrackEntity(playlistId, id, idx) }
        )

        val newOrder = listOf(50L, 30L, 10L, 40L, 20L)
        db.playlistTrackDao().reorder(playlistId, newOrder)

        val rows = db.playlistTrackDao().observeForPlaylist(playlistId).first()
        assertEquals(newOrder, rows.map { it.mediaStoreId })
        assertEquals(listOf(0, 1, 2, 3, 4), rows.map { it.position })
    }

    @Test
    fun reorderWithEmptyListIsNoop() = runTest {
        val playlistId = db.playlistDao().insert(PlaylistEntity(name = "P", createdAt = 1L))
        db.trackDao().upsert(TrackEntity(1L, "T", null, null, null, 1L, "u://1", 1L))
        db.playlistTrackDao().insertIgnore(PlaylistTrackEntity(playlistId, 1L, 0))

        db.playlistTrackDao().reorder(playlistId, emptyList())

        val rows = db.playlistTrackDao().observeForPlaylist(playlistId).first()
        assertEquals(listOf(0), rows.map { it.position })
    }
}
