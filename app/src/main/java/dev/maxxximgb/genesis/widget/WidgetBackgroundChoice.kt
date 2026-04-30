package dev.maxxximgb.genesis.widget

import android.graphics.Color
import androidx.core.graphics.ColorUtils

enum class WidgetBackgroundChoice {
    Dark,
    Light;

    companion object {
        const val MODE_DARK = 0
        const val MODE_LIGHT = 1

        private const val MUTED_ALPHA = 0.65f

        // Neutral, eye-friendly off-black / off-white. Not pure #000 / #FFF.
        private const val DARK_BG = 0xFF1A1A1A.toInt()
        private const val LIGHT_BG = 0xFFF5F5F5.toInt()

        fun colorsFor(choice: WidgetBackgroundChoice): WidgetColors = when (choice) {
            Dark -> WidgetColors(
                background = DARK_BG,
                onBackground = Color.WHITE,
                onBackgroundMuted = ColorUtils.setAlphaComponent(Color.WHITE, mutedAlpha()),
            )
            Light -> WidgetColors(
                background = LIGHT_BG,
                onBackground = 0xFF1A1A1A.toInt(),
                onBackgroundMuted = ColorUtils.setAlphaComponent(0xFF1A1A1A.toInt(), mutedAlpha()),
            )
        }

        private fun mutedAlpha(): Int = (MUTED_ALPHA * 255f).toInt()
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
