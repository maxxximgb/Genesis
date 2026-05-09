package dev.maxxximgb.genesis.domain.audiofx

/**
 * User-defined snapshot of all audio-effect controls. Identified by [id] (UUID), so renaming
 * a preset doesn't break the active-preset reference held in [UserPreferencesStore].
 *
 * Band levels are stored in millibels (1 dB = 100 mB), matching the Android `Equalizer` API's
 * native unit. Bass-boost and virtualizer strengths are 0..1000 permille — also the native unit.
 */
data class EqualizerPreset(
    val id: String,
    val name: String,
    val bandLevelsMillibels: List<Int>,
) {
    val isDefault: Boolean get() = id == DEFAULT_ID

    companion object {
        const val MAX_NAME_LENGTH = 32

        /**
         * Reserved id for the always-present default preset. The default preset behaves like
         * any user preset (modifiable, persistable) but the UI hides its rename/delete icons —
         * the dropdown always shows it as the first item.
         */
        const val DEFAULT_ID = "__default__"

        fun flatDefault(numberOfBands: Int = 5): EqualizerPreset = EqualizerPreset(
            id = DEFAULT_ID,
            name = "",
            bandLevelsMillibels = List(numberOfBands) { 0 },
        )
    }
}
