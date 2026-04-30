package dev.maxxximgb.genesis.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.maxxximgb.genesis.widget.WidgetBackgroundChoice
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

    private lateinit var store: WidgetPreferencesStore

    @Before
    fun setUp() {
        val file = File(tempFolder.newFolder(), "widget_prefs.preferences_pb")
        val ds = PreferenceDataStoreFactory.create(produceFile = { file })
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
}
