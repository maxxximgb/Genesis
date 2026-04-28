package dev.maxxximgb.genesis.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.maxxximgb.genesis.data.local.entity.PlaylistEntity
import dev.maxxximgb.genesis.data.local.entity.PlaylistTrackEntity
import dev.maxxximgb.genesis.data.local.entity.TrackEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistTrackDedupTest {

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
    fun insertingSamePairTwiceKeepsOneRow() = runTest {
        val playlistId = db.playlistDao().insert(PlaylistEntity(name = "P", createdAt = 1L))
        db.trackDao().upsert(TrackEntity(7L, "Song", null, null, null, 1000L, "u://7", 1L))

        db.playlistTrackDao().insertIgnore(PlaylistTrackEntity(playlistId, 7L, 0))
        db.playlistTrackDao().insertIgnore(PlaylistTrackEntity(playlistId, 7L, 99))

        assertEquals(1, db.playlistTrackDao().countForPlaylist(playlistId))
    }
}
