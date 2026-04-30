package dev.maxxximgb.genesis.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.maxxximgb.genesis.domain.model.LoopState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PlaylistModeStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: PlaylistModeStore
    private lateinit var dataStoreFile: File

    @Before
    fun setUp() {
        dataStoreFile = File(tempFolder.newFolder(), "playlist_modes.preferences_pb")
        val ds = PreferenceDataStoreFactory.create(produceFile = { dataStoreFile })
        store = PlaylistModeStore(ds)
    }

    @Test
    fun defaultsToOffForUnknownPlaylist() = runTest {
        assertEquals(LoopState.OFF, store.getMode(123L))
        assertEquals(LoopState.OFF, store.observeMode(123L).first())
    }

    @Test
    fun setAndGetPersistsPerPlaylist() = runTest {
        store.setMode(1L, LoopState.SHUFFLE)
        store.setMode(2L, LoopState.REPEAT_ONE)

        assertEquals(LoopState.SHUFFLE, store.getMode(1L))
        assertEquals(LoopState.REPEAT_ONE, store.getMode(2L))
        assertEquals(LoopState.OFF, store.getMode(3L))
    }

    @Test
    fun observeReflectsChanges() = runTest {
        store.setMode(7L, LoopState.REPEAT_ALL)
        assertEquals(LoopState.REPEAT_ALL, store.observeMode(7L).first())

        store.setMode(7L, LoopState.SHUFFLE)
        assertEquals(LoopState.SHUFFLE, store.observeMode(7L).first())
    }
}
