package dev.maxxximgb.genesis.data.audiofx

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.maxxximgb.genesis.di.EqualizerPresets
import dev.maxxximgb.genesis.domain.audiofx.EqualizerPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerPresetRepository @Inject constructor(
    @EqualizerPresets private val dataStore: DataStore<Preferences>,
) {

    /**
     * Always emits at least the default preset. If the user hasn't yet edited the default,
     * a flat synthetic record is prepended in-memory; the moment they tweak it,
     * [savePreset] persists it like any other preset record.
     */
    fun observePresets(): Flow<List<EqualizerPreset>> = dataStore.data
        .map { prefs ->
            val stored = deserializePresets(prefs[Keys.PRESETS_JSON])
            if (stored.any { it.id == EqualizerPreset.DEFAULT_ID }) {
                stored
            } else {
                listOf(EqualizerPreset.flatDefault()) + stored
            }
        }
        .distinctUntilChanged()

    suspend fun savePreset(preset: EqualizerPreset): EqualizerPreset {
        // Default preset keeps its reserved id; every other preset gets a UUID if it doesn't
        // already have one (new save) or its existing id is reused (in-place edit).
        val toStore = preset.copy(
            id = when {
                preset.id == EqualizerPreset.DEFAULT_ID -> EqualizerPreset.DEFAULT_ID
                preset.id.isBlank() -> UUID.randomUUID().toString()
                else -> preset.id
            },
            name = preset.name.take(EqualizerPreset.MAX_NAME_LENGTH),
        )
        dataStore.edit { prefs ->
            val list = deserializePresets(prefs[Keys.PRESETS_JSON]).toMutableList()
            val idx = list.indexOfFirst { it.id == toStore.id }
            if (idx >= 0) list[idx] = toStore else list.add(toStore)
            prefs[Keys.PRESETS_JSON] = serializePresets(list)
        }
        return toStore
    }

    suspend fun renamePreset(id: String, newName: String) {
        // Default preset's name is provided by the localized string resource — block renames
        // on it so the row label stays in sync with the user's chosen language.
        if (id == EqualizerPreset.DEFAULT_ID) return
        val trimmed = newName.take(EqualizerPreset.MAX_NAME_LENGTH)
        dataStore.edit { prefs ->
            val list = deserializePresets(prefs[Keys.PRESETS_JSON])
                .map { if (it.id == id) it.copy(name = trimmed) else it }
            prefs[Keys.PRESETS_JSON] = serializePresets(list)
        }
    }

    suspend fun deletePreset(id: String) {
        // Default preset is permanent — deletion would just resurrect a flat copy on the next
        // observe, but it would discard the user's edits to it. Treat as no-op.
        if (id == EqualizerPreset.DEFAULT_ID) return
        dataStore.edit { prefs ->
            val list = deserializePresets(prefs[Keys.PRESETS_JSON]).filterNot { it.id == id }
            prefs[Keys.PRESETS_JSON] = serializePresets(list)
        }
    }

    /**
     * Hook for future import UI / adb-side bootstrap. Reads a JSON file in the same format
     * this repo writes (`[{"id":..., "name":..., "bands":[...], "bass":..., "virt":..., "spatial":...}, ...]`)
     * and merges into the existing list. Imported presets keep their ids when unique; if an
     * imported name collides with an existing preset (different id), a `(2)` suffix is added.
     * Returns the count of presets actually added.
     */
    suspend fun loadFromFile(file: File): Result<Int> = runCatching {
        val raw = file.readText()
        val incoming = deserializePresets(raw)
        if (incoming.isEmpty()) return@runCatching 0
        var added = 0
        dataStore.edit { prefs ->
            val current = deserializePresets(prefs[Keys.PRESETS_JSON]).toMutableList()
            val existingIds = current.map { it.id }.toMutableSet()
            val existingNames = current.map { it.name }.toMutableSet()
            for (preset in incoming) {
                if (preset.id in existingIds) continue
                val uniqueName = uniquify(preset.name, existingNames)
                val toAdd = preset.copy(name = uniqueName)
                current += toAdd
                existingIds += toAdd.id
                existingNames += uniqueName
                added++
            }
            prefs[Keys.PRESETS_JSON] = serializePresets(current)
        }
        added
    }

    private fun uniquify(base: String, taken: Set<String>): String {
        if (base !in taken) return base
        var n = 2
        while ("$base ($n)" in taken) n++
        return "$base ($n)"
    }

    private object Keys {
        val PRESETS_JSON = stringPreferencesKey("equalizer_presets_json")
    }

    companion object {
        /** Serializes presets to a stable JSON shape. Internal but exposed for tests. */
        fun serializePresets(presets: List<EqualizerPreset>): String {
            val arr = JSONArray()
            for (p in presets) {
                val obj = JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("bands", JSONArray(p.bandLevelsMillibels))
                arr.put(obj)
            }
            return arr.toString()
        }

        /**
         * Tolerant deserializer — older saves with `bass`/`virt`/`spatial` keys still parse,
         * those keys are just ignored. Keeps existing on-device data forward-compatible after
         * the BassBoost/Virtualizer/Spatializer features were removed.
         */
        fun deserializePresets(raw: String?): List<EqualizerPreset> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val bands = o.optJSONArray("bands") ?: JSONArray()
                        val list = ArrayList<Int>(bands.length())
                        for (j in 0 until bands.length()) list += bands.optInt(j, 0)
                        add(
                            EqualizerPreset(
                                id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                                name = o.optString("name", "").take(EqualizerPreset.MAX_NAME_LENGTH),
                                bandLevelsMillibels = list,
                            ),
                        )
                    }
                }
            } catch (e: JSONException) {
                emptyList()
            }
        }
    }
}
