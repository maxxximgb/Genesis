package dev.maxxximgb.genesis.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import dev.maxxximgb.genesis.widget.WidgetBackgroundChoice
import dev.maxxximgb.genesis.widget.WidgetTarget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
class WidgetPreferencesStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var ds: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var store: WidgetPreferencesStore

    @Before
    fun setUp() {
        val file = File(tempFolder.newFolder(), "widget_prefs.preferences_pb")
        ds = PreferenceDataStoreFactory.create(produceFile = { file })
        store = WidgetPreferencesStore(ds)
    }

    @Test
    fun unboundWidgetIsNull() = runTest {
        assertNull(store.getPlaylistFor(42))
        assertNull(store.observePlaylistFor(42).first())
    }

    @Test
    fun setAndGetPersistsPerWidget() = runTest {
        store.setPlaylistFor(1, 100L)
        store.setPlaylistFor(2, 200L)

        assertEquals(100L, store.getPlaylistFor(1))
        assertEquals(200L, store.getPlaylistFor(2))
        assertNull(store.getPlaylistFor(3))
    }

    @Test
    fun clearRemovesBinding() = runTest {
        store.setPlaylistFor(5, 500L)
        store.clear(5)
        assertNull(store.getPlaylistFor(5))
    }

    @Test
    fun observeReflectsLatestValue() = runTest {
        store.setPlaylistFor(10, 1L)
        assertEquals(1L, store.observePlaylistFor(10).first())
        store.setPlaylistFor(10, 2L)
        assertEquals(2L, store.observePlaylistFor(10).first())
    }

    @Test
    fun storedBackgroundIsNullWhenUnset() = runTest {
        assertNull(store.getStoredBackgroundFor(42))
        // observe variant exposes a Dark fallback so the widget render path always has a value.
        assertEquals(WidgetBackgroundChoice.Dark, store.observeBackgroundFor(42).first())
    }

    @Test
    fun backgroundIsIndependentPerWidget() = runTest {
        store.setBackgroundFor(1, WidgetBackgroundChoice.Dark)
        store.setBackgroundFor(2, WidgetBackgroundChoice.Light)

        assertEquals(WidgetBackgroundChoice.Dark, store.getStoredBackgroundFor(1))
        assertEquals(WidgetBackgroundChoice.Light, store.getStoredBackgroundFor(2))
        assertNull(store.getStoredBackgroundFor(3))
    }

    @Test
    fun backgroundChoiceRoundTrips() = runTest {
        store.setBackgroundFor(7, WidgetBackgroundChoice.Light)
        assertEquals(WidgetBackgroundChoice.Light, store.getStoredBackgroundFor(7))
        assertEquals(WidgetBackgroundChoice.Light, store.observeBackgroundFor(7).first())

        store.setBackgroundFor(7, WidgetBackgroundChoice.Dark)
        assertEquals(WidgetBackgroundChoice.Dark, store.getStoredBackgroundFor(7))
    }

    @Test
    fun clearRemovesPlaylistAndBackground() = runTest {
        store.setPlaylistFor(11, 100L)
        store.setBackgroundFor(11, WidgetBackgroundChoice.Light)
        store.clear(11)
        assertNull(store.getPlaylistFor(11))
        assertNull(store.getStoredBackgroundFor(11))
    }

    @Test
    fun audiobookTargetRoundTrips() = runTest {
        store.setTarget(20, WidgetTarget.Audiobooks)

        assertEquals(WidgetTarget.Audiobooks, store.getTargetFor(20))
        assertEquals(WidgetTarget.Audiobooks, store.observeTargetFor(20).first())
        // Legacy playlist API returns null for non-playlist targets so old code paths
        // don't accidentally treat the audiobook queue as a playlist id.
        assertNull(store.getPlaylistFor(20))
    }

    @Test
    fun playlistTargetRoundTrips() = runTest {
        store.setTarget(21, WidgetTarget.Playlist(7L))

        assertEquals(WidgetTarget.Playlist(7L), store.getTargetFor(21))
        assertEquals(7L, store.getPlaylistFor(21))
    }

    @Test
    fun legacyLongOnlyPlaylistKeyReadsBackAsTarget() = runTest {
        // Simulate a widget bound before 2.3 — only the long-typed playlist key is present,
        // the new "widget_target_*" string key is absent.
        ds.edit { it[longPreferencesKey("widget_playlist_99")] = 555L }

        assertEquals(WidgetTarget.Playlist(555L), store.getTargetFor(99))
        assertEquals(555L, store.getPlaylistFor(99))
    }

    @Test
    fun setAudiobookTargetClearsLegacyPlaylistKey() = runTest {
        store.setPlaylistFor(30, 7L)
        store.setTarget(30, WidgetTarget.Audiobooks)

        assertEquals(WidgetTarget.Audiobooks, store.getTargetFor(30))
        assertNull(store.getPlaylistFor(30))
    }
}
