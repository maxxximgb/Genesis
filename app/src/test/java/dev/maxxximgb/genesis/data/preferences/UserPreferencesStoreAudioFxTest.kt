package dev.maxxximgb.genesis.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.maxxximgb.genesis.domain.audiofx.AudioFxState
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
class UserPreferencesStoreAudioFxTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: UserPreferencesStore

    @Before
    fun setUp() {
        val file = File(tempFolder.newFolder(), "app_prefs.preferences_pb")
        val ds = PreferenceDataStoreFactory.create(produceFile = { file })
        store = UserPreferencesStore(ds)
    }

    @Test
    fun freshStoreReturnsDefaultAudioFxState() = runTest {
        assertEquals(AudioFxState.Default, store.observeAudioFxState().first())
    }

    @Test
    fun individualSettersPersist() = runTest {
        store.setAudioFxEnabled(true)
        store.setAudioFxActivePresetId("abc-123")
        store.setAudioFxBands(listOf(300, -100, 0, 200, 500))

        val state = store.observeAudioFxState().first()
        assertEquals(true, state.masterEnabled)
        assertEquals("abc-123", state.activePresetId)
        assertEquals(listOf(300, -100, 0, 200, 500), state.bandLevelsMillibels)
    }

    @Test
    fun activePresetIdNullClearsKey() = runTest {
        store.setAudioFxActivePresetId("xyz")
        assertEquals("xyz", store.observeAudioFxState().first().activePresetId)

        store.setAudioFxActivePresetId(null)
        assertEquals(null, store.observeAudioFxState().first().activePresetId)
    }

    @Test
    fun applyPresetSnapshotIsAtomic() = runTest {
        store.applyAudioFxPresetSnapshot(
            activePresetId = "p1",
            bands = listOf(100, 200, 300),
        )

        val state = store.observeAudioFxState().first()
        assertEquals("p1", state.activePresetId)
        assertEquals(listOf(100, 200, 300), state.bandLevelsMillibels)
    }
}
