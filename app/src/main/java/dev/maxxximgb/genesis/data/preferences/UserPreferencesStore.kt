package dev.maxxximgb.genesis.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.maxxximgb.genesis.di.UserPreferences
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.ui.library.AlbumsLayout
import dev.maxxximgb.genesis.ui.theme.PaletteId
import dev.maxxximgb.genesis.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesStore @Inject constructor(
    @UserPreferences private val dataStore: DataStore<Preferences>,
) {

    fun observeLibrarySort(): Flow<SortOrder> = dataStore.data
        .map { prefs ->
            prefs[Keys.LIBRARY_SORT]
                ?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
                ?: SortOrder.DATE_ADDED_DESC
        }
        .distinctUntilChanged()

    suspend fun setLibrarySort(sort: SortOrder) {
        dataStore.edit { it[Keys.LIBRARY_SORT] = sort.name }
    }

    fun observeThemeMode(): Flow<ThemeMode> = dataStore.data
        .map { ThemeMode.fromName(it[Keys.THEME_MODE]) }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    fun observePalette(): Flow<PaletteId> = dataStore.data
        .map { PaletteId.fromName(it[Keys.PALETTE_ID]) }
        .distinctUntilChanged()

    suspend fun setPalette(palette: PaletteId) {
        dataStore.edit { it[Keys.PALETTE_ID] = palette.name }
    }

    fun observeFontScale(): Flow<Float> = dataStore.data
        .map { it[Keys.FONT_SCALE] ?: DEFAULT_FONT_SCALE }
        .distinctUntilChanged()

    suspend fun setFontScale(scale: Float) {
        dataStore.edit { it[Keys.FONT_SCALE] = scale }
    }

    fun observeAlbumsLayout(): Flow<AlbumsLayout> = dataStore.data
        .map { prefs ->
            prefs[Keys.ALBUMS_LAYOUT]
                ?.let { runCatching { AlbumsLayout.valueOf(it) }.getOrNull() }
                ?: AlbumsLayout.LIST
        }
        .distinctUntilChanged()

    suspend fun setAlbumsLayout(layout: AlbumsLayout) {
        dataStore.edit { it[Keys.ALBUMS_LAYOUT] = layout.name }
    }

    private object Keys {
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PALETTE_ID = stringPreferencesKey("palette_id")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val ALBUMS_LAYOUT = stringPreferencesKey("albums_layout")
    }

    companion object {
        const val DEFAULT_FONT_SCALE = 1.0f
        val FONT_SCALE_OPTIONS = listOf(0.85f, 1.0f, 1.15f, 1.30f)
    }
}
