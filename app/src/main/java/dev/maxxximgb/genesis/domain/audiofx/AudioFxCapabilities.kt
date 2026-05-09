package dev.maxxximgb.genesis.domain.audiofx

/**
 * What the current device's audio hardware actually supports. Read once at controller attach
 * time. UI uses this to decide which sliders/toggles to render and what ranges to honor.
 */
data class AudioFxCapabilities(
    val equalizerSupported: Boolean,
    val numberOfBands: Int,
    val bandCenterFrequenciesHz: List<Int>,
    val bandLevelRangeMillibels: IntRange,
) {
    companion object {
        val Unsupported = AudioFxCapabilities(
            equalizerSupported = false,
            numberOfBands = 0,
            bandCenterFrequenciesHz = emptyList(),
            bandLevelRangeMillibels = 0..0,
        )
    }
}
