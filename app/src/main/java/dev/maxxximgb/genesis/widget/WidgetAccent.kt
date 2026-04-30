package dev.maxxximgb.genesis.widget

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

/**
 * Color triplet derived from album art for tinting the widget surface.
 * All values are ARGB Ints.
 */
data class WidgetAccent(
    val background: Int,
    val onBackground: Int,
    val onBackgroundMuted: Int,
)

object WidgetAccentExtractor {

    private const val MUTED_ALPHA = 0.65f

    /**
     * Returns null when [bitmap] is null or Palette can't find a usable swatch.
     * Caller is responsible for falling back to theme colors in that case.
     */
    fun extract(bitmap: Bitmap?): WidgetAccent? {
        if (bitmap == null) return null
        val palette = Palette.from(bitmap).clearFilters().generate()
        val swatch = palette.vibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.darkMutedSwatch
            ?: palette.lightMutedSwatch
            ?: palette.dominantSwatch
            ?: return null
        val background = swatch.rgb
        val onBackground = pickOnColor(background)
        val muted = ColorUtils.setAlphaComponent(
            onBackground,
            (MUTED_ALPHA * 255f).toInt(),
        )
        return WidgetAccent(
            background = background,
            onBackground = onBackground,
            onBackgroundMuted = muted,
        )
    }

    fun fromSolid(background: Int): WidgetAccent {
        val onBackground = pickOnColor(background)
        val muted = ColorUtils.setAlphaComponent(onBackground, (MUTED_ALPHA * 255f).toInt())
        return WidgetAccent(
            background = background,
            onBackground = onBackground,
            onBackgroundMuted = muted,
        )
    }

    internal fun pickOnColor(background: Int): Int {
        val whiteContrast = ColorUtils.calculateContrast(Color.WHITE, background)
        val blackContrast = ColorUtils.calculateContrast(Color.BLACK, background)
        return if (whiteContrast >= blackContrast) Color.WHITE else Color.BLACK
    }
}
