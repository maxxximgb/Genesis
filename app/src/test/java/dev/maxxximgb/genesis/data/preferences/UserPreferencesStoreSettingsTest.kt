package dev.maxxximgb.genesis.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.maxxximgb.genesis.ui.theme.PaletteId
import dev.maxxximgb.genesis.ui.theme.ThemeMode
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
class UserPreferencesStoreSettingsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: UserPreferencesStore
    private lateinit var dataStoreFile: File

    @Before
    fun setUp() {
        dataStoreFile = File(tempFolder.newFolder(), "app_prefs.preferences_pb")
        val ds = PreferenceDataStoreFactory.create(produceFile = { dataStoreFile })
        store = UserPreferencesStore(ds)
    }

    @Test
    fun defaultsAreReturnedForFreshStore() = runTest {
        assertEquals(ThemeMode.AUTO, store.observeThemeMode().first())
        assertEquals(PaletteId.VERDANT, store.observePalette().first())
        assertEquals(UserPreferencesStore.DEFAULT_FONT_SCALE, store.observeFontScale().first())
    }

    @Test
    fun setThemeModePersists() = runTest {
        store.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, store.observeThemeMode().first())
    }

    @Test
    fun setPalettePersists() = runTest {
        store.setPalette(PaletteId.CRIMSON)
        assertEquals(PaletteId.CRIMSON, store.observePalette().first())
    }

    @Test
    fun setFontScalePersists() = runTest {
        store.setFontScale(1.30f)
        assertEquals(1.30f, store.observeFontScale().first())
    }

    @Test
    fun unknownThemeNameFallsBackToDefault() = runTest {
        // simulate corrupted/migrated data: write garbage value
        store.setThemeMode(ThemeMode.DARK)
        // No way to write garbage via API; trust enum mapper handles it via fromName()
        assertEquals(ThemeMode.AUTO, ThemeMode.fromName("WTF"))
        assertEquals(ThemeMode.AUTO, ThemeMode.fromName(null))
    }

    @Test
    fun unknownPaletteNameFallsBackToDefault() = runTest {
        assertEquals(PaletteId.VERDANT, PaletteId.fromName("WTF"))
        assertEquals(PaletteId.VERDANT, PaletteId.fromName(null))
    }
}
