package dev.maxxximgb.genesis.data.playback

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.RepeatMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PlaybackStateStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: PlaybackStateStore
    private lateinit var dataStoreFile: File

    @Before
    fun setUp() {
        dataStoreFile = File(tempFolder.newFolder(), "playback_state.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { dataStoreFile })
        store = PlaybackStateStore(dataStore)
    }

    @After
    fun tearDown() {
        // PreferenceDataStoreFactory.create has no explicit close — temp folder cleanup handles file removal.
    }

    @Test
    fun freshStoreYieldsDefaultPlaybackState() = runTest {
        val state = store.flow.first()
        assertEquals(PlaybackState(), state)
    }

    @Test
    fun updatePersistsAllFields() = runTest {
        val written = PlaybackState(
            isPlaying = true,
            title = "Song",
            artist = "Artist",
            currentMediaStoreId = 42L,
            playlistId = 7L,
            queue = listOf(10L, 20L, 30L),
            currentIndex = 1,
            positionMs = 12_345L,
            durationMs = 200_000L,
            shuffleEnabled = true,
            repeatMode = RepeatMode.ONE,
        )
        store.update(written)

        val read = store.flow.first()
        assertEquals(written, read)
    }

    @Test
    fun updateWithNullableFieldsRemovesKeys() = runTest {
        store.update(
            PlaybackState(
                title = "X",
                artist = "Y",
                currentMediaStoreId = 1L,
                playlistId = 2L,
            )
        )
        store.update(PlaybackState())

        val read = store.flow.first()
        assertNull(read.title)
        assertNull(read.artist)
        assertNull(read.currentMediaStoreId)
        assertNull(read.playlistId)
    }

    @Test
    fun setPositionUpdatesOnlyPosition() = runTest {
        store.update(
            PlaybackState(
                title = "X",
                queue = listOf(1L, 2L),
                currentIndex = 0,
                positionMs = 100L,
            )
        )

        store.setPosition(99_999L)

        val read = store.flow.first()
        assertEquals(99_999L, read.positionMs)
        assertEquals("X", read.title)
        assertEquals(listOf(1L, 2L), read.queue)
    }
}
