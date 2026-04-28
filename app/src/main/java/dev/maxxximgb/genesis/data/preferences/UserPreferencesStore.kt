package dev.maxxximgb.genesis.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.maxxximgb.genesis.di.UserPreferences
import dev.maxxximgb.genesis.domain.model.SortOrder
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

    private object Keys {
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
    }
}
