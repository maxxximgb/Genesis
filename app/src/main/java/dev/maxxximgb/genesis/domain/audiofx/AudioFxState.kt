package dev.maxxximgb.genesis.domain.audiofx

/**
 * Live state of the audio-effects pipeline as persisted in [UserPreferencesStore].
 *
 * [activePresetId] = null is the sentinel "По умолчанию" — flat values, no saved preset is the
 * source of truth. Moving a slider while a saved preset is active resets [activePresetId] to
 * null (the slider values diverge from the preset's stored snapshot).
 */
data class AudioFxState(
    val masterEnabled: Boolean,
    val activePresetId: String?,
    val bandLevelsMillibels: List<Int>,
) {
    companion object {
        val Default = AudioFxState(
            masterEnabled = false,
            activePresetId = null,
            bandLevelsMillibels = emptyList(),
        )
    }
}
