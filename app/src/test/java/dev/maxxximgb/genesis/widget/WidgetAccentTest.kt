package dev.maxxximgb.genesis.widget

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetAccentTest {

    @Test
    fun `null bitmap returns null accent`() {
        assertNull(WidgetAccentExtractor.extract(null))
    }

    @Test
    fun `solid red bitmap returns red-hued background`() {
        val accent = WidgetAccentExtractor.extract(solidColorBitmap(Color.RED))
        assertNotNull(accent)
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(accent!!.background, hsl)
        // Red hue ≈ 0°; allow ±20° wrap (Palette can quantise slightly).
        val hue = hsl[0]
        val isRedHue = hue <= 20f || hue >= 340f
        assertTrue("Expected red hue, got $hue°", isRedHue)
    }

    @Test
    fun `greyscale bitmap returns non-null accent with black or white onBackground`() {
        val accent = WidgetAccentExtractor.extract(solidColorBitmap(Color.rgb(120, 120, 120)))
        assertNotNull(accent)
        val on = accent!!.onBackground
        // pickOnColor must choose either pure white or pure black; nothing in between.
        val isPureBlackOrWhite = on == Color.WHITE || on == Color.BLACK
        assertTrue("onBackground must be pure black or white, got #${Integer.toHexString(on)}", isPureBlackOrWhite)
    }

    @Test
    fun `onBackground meets WCAG AA contrast against background`() {
        val cases = listOf(
            Color.RED,
            Color.rgb(20, 20, 20),
            Color.rgb(240, 240, 240),
            Color.rgb(0, 100, 200),
            Color.rgb(255, 200, 0),
        )
        cases.forEach { color ->
            val accent = WidgetAccentExtractor.extract(solidColorBitmap(color))
            assertNotNull("Palette returned no swatch for color $color", accent)
            val contrast = ColorUtils.calculateContrast(accent!!.onBackground, accent.background)
            assertTrue(
                "Contrast for #${Integer.toHexString(color)} was $contrast (< 4.5)",
                contrast >= 4.5,
            )
        }
    }

    @Test
    fun `muted onBackground keeps same RGB as onBackground but lower alpha`() {
        val accent = WidgetAccentExtractor.extract(solidColorBitmap(Color.RED))
        assertNotNull(accent)
        val on = accent!!.onBackground
        val muted = accent.onBackgroundMuted
        assertEquals(Color.red(on), Color.red(muted))
        assertEquals(Color.green(on), Color.green(muted))
        assertEquals(Color.blue(on), Color.blue(muted))
        assertTrue("Muted alpha must be < onBackground alpha", Color.alpha(muted) < Color.alpha(on))
    }

    private fun solidColorBitmap(color: Int, size: Int = 64): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap
    }
}
