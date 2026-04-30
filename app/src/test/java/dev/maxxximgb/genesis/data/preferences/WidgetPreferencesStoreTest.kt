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
    fun backgroundDefaultsToDynamicWhenUnset() = runTest {
        assertEquals(WidgetBackgroundChoice.Dynamic, store.getBackgroundFor(42))
        assertEquals(WidgetBackgroundChoice.Dynamic, store.observeBackgroundFor(42).first())
    }

    @Test
    fun backgroundIsIndependentPerWidget() = runTest {
        store.setBackgroundFor(1, WidgetBackgroundChoice.Theme)
        store.setBackgroundFor(2, WidgetBackgroundChoice.Solid(0xFFE0506B.toInt()))

        assertEquals(WidgetBackgroundChoice.Theme, store.getBackgroundFor(1))
        assertEquals(WidgetBackgroundChoice.Solid(0xFFE0506B.toInt()), store.getBackgroundFor(2))
        assertEquals(WidgetBackgroundChoice.Dynamic, store.getBackgroundFor(3))
    }

    @Test
    fun solidColorRoundTripsExactArgb() = runTest {
        val choice = WidgetBackgroundChoice.Solid(0xFF8246C8.toInt())
        store.setBackgroundFor(7, choice)
        assertEquals(choice, store.getBackgroundFor(7))
    }

    @Test
    fun switchingFromSolidToDynamicClearsColor() = runTest {
        store.setBackgroundFor(9, WidgetBackgroundChoice.Solid(0xFF3D6EE0.toInt()))
        store.setBackgroundFor(9, WidgetBackgroundChoice.Dynamic)
        assertEquals(WidgetBackgroundChoice.Dynamic, store.getBackgroundFor(9))
    }

    @Test
    fun clearRemovesPlaylistAndBackground() = runTest {
        store.setPlaylistFor(11, 100L)
        store.setBackgroundFor(11, WidgetBackgroundChoice.Solid(0xFF7FAE3A.toInt()))
        store.clear(11)
        assertNull(store.getPlaylistFor(11))
        assertEquals(WidgetBackgroundChoice.Dynamic, store.getBackgroundFor(11))
    }
}
