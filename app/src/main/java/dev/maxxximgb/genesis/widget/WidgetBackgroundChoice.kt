package dev.maxxximgb.genesis.widget

import android.graphics.Color

enum class WidgetBackgroundChoice {
    Dark,
    Light;

    companion object {
        const val MODE_DARK = 0
        const val MODE_LIGHT = 1

        // Neutral, eye-friendly off-black / off-white. Not pure #000 / #FFF.
        private const val DARK_BG = 0xFF1A1A1A.toInt()
        private const val LIGHT_BG = 0xFFF5F5F5.toInt()

        // Solid gray for inactive icons / muted text. Earlier alpha-based muted didn't
        // visibly dim icons through Glance → RemoteViews ColorFilter.tint on some launchers,
        // so we use a flat color the launcher can't accidentally re-saturate.
        private const val DARK_MUTED = 0xFF6E6E6E.toInt()
        private const val LIGHT_MUTED = 0xFF8E8E8E.toInt()

        fun colorsFor(choice: WidgetBackgroundChoice): WidgetColors = when (choice) {
            Dark -> WidgetColors(
                background = DARK_BG,
                onBackground = Color.WHITE,
                onBackgroundMuted = DARK_MUTED,
            )
            Light -> WidgetColors(
                background = LIGHT_BG,
                onBackground = 0xFF1A1A1A.toInt(),
                onBackgroundMuted = LIGHT_MUTED,
            )
        }
    }
}

data class WidgetColors(
    val background: Int,
    val onBackground: Int,
    val onBackgroundMuted: Int,
)

fun WidgetBackgroundChoice.encodeMode(): Int = when (this) {
    WidgetBackgroundChoice.Dark -> WidgetBackgroundChoice.MODE_DARK
    WidgetBackgroundChoice.Light -> WidgetBackgroundChoice.MODE_LIGHT
}

/** Returns null when [mode] is missing. Caller decides the default policy. */
fun decodeBackgroundChoice(mode: Int?): WidgetBackgroundChoice? = when (mode) {
    WidgetBackgroundChoice.MODE_DARK -> WidgetBackgroundChoice.Dark
    WidgetBackgroundChoice.MODE_LIGHT -> WidgetBackgroundChoice.Light
    else -> null
}
