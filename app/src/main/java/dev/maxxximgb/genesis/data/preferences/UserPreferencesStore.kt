package dev.maxxximgb.genesis.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.maxxximgb.genesis.di.UserPreferences
import dev.maxxximgb.genesis.domain.audiofx.AudioFxState
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

    /**
     * User-flagged audiobook tracks. We can't write `MediaStore.IS_AUDIOBOOK` for files we
     * didn't create without a security IntentSender flow, so the override list lives here
     * and is unioned with native audiobooks at query time.
     */
    fun observeAudiobookOverrides(): Flow<Set<Long>> = dataStore.data
        .map { prefs -> prefs[Keys.AUDIOBOOK_OVERRIDES].decodeIds() }
        .distinctUntilChanged()

    suspend fun toggleAudiobookOverride(mediaStoreId: Long) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.AUDIOBOOK_OVERRIDES].decodeIds().toMutableSet()
            if (mediaStoreId in current) current.remove(mediaStoreId) else current.add(mediaStoreId)
            prefs[Keys.AUDIOBOOK_OVERRIDES] = current.joinToString(",")
        }
    }

    /** Bulk-add ids to the audiobook override set (idempotent). */
    suspend fun addAudiobookOverrides(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        dataStore.edit { prefs ->
            val current = prefs[Keys.AUDIOBOOK_OVERRIDES].decodeIds().toMutableSet()
            current.addAll(ids)
            prefs[Keys.AUDIOBOOK_OVERRIDES] = current.joinToString(",")
        }
    }

    /** Bulk-remove ids from the audiobook override set. Native IS_AUDIOBOOK rows unaffected. */
    suspend fun removeAudiobookOverrides(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        dataStore.edit { prefs ->
            val current = prefs[Keys.AUDIOBOOK_OVERRIDES].decodeIds().toMutableSet()
            current.removeAll(ids.toSet())
            prefs[Keys.AUDIOBOOK_OVERRIDES] = current.joinToString(",")
        }
    }

    private fun String?.decodeIds(): Set<Long> =
        this?.split(',')?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()

    fun observeAudioFxState(): Flow<AudioFxState> = dataStore.data
        .map { prefs ->
            AudioFxState(
                masterEnabled = prefs[Keys.AUDIOFX_ENABLED] ?: false,
                activePresetId = prefs[Keys.AUDIOFX_ACTIVE_PRESET_ID]?.takeIf { it.isNotBlank() },
                bandLevelsMillibels = prefs[Keys.AUDIOFX_EQ_BANDS].decodeBands(),
            )
        }
        .distinctUntilChanged()

    suspend fun setAudioFxEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUDIOFX_ENABLED] = enabled }
    }

    suspend fun setAudioFxActivePresetId(id: String?) {
        dataStore.edit {
            if (id == null) it.remove(Keys.AUDIOFX_ACTIVE_PRESET_ID)
            else it[Keys.AUDIOFX_ACTIVE_PRESET_ID] = id
        }
    }

    suspend fun setAudioFxBands(bands: List<Int>) {
        dataStore.edit { it[Keys.AUDIOFX_EQ_BANDS] = bands.joinToString(",") }
    }

    /** Atomic write for both EQ fields — used when applying a saved preset. */
    suspend fun applyAudioFxPresetSnapshot(
        activePresetId: String?,
        bands: List<Int>,
    ) {
        dataStore.edit { prefs ->
            if (activePresetId == null) prefs.remove(Keys.AUDIOFX_ACTIVE_PRESET_ID)
            else prefs[Keys.AUDIOFX_ACTIVE_PRESET_ID] = activePresetId
            prefs[Keys.AUDIOFX_EQ_BANDS] = bands.joinToString(",")
        }
    }

    private fun String?.decodeBands(): List<Int> =
        this?.takeIf { it.isNotBlank() }
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: emptyList()

    private object Keys {
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PALETTE_ID = stringPreferencesKey("palette_id")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val ALBUMS_LAYOUT = stringPreferencesKey("albums_layout")
        val AUDIOBOOK_OVERRIDES = stringPreferencesKey("audiobook_overrides")
        val AUDIOFX_ENABLED = booleanPreferencesKey("audiofx_enabled")
        val AUDIOFX_ACTIVE_PRESET_ID = stringPreferencesKey("audiofx_active_preset_id")
        val AUDIOFX_EQ_BANDS = stringPreferencesKey("audiofx_eq_bands")
    }

    companion object {
        const val DEFAULT_FONT_SCALE = 1.0f
        val FONT_SCALE_OPTIONS = listOf(0.85f, 1.0f, 1.15f, 1.30f)
    }
}
