package dev.maxxximgb.genesis.widget

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetBackgroundChoiceTest {

    @Test
    fun `dynamic encodes mode and decodes back`() {
        val mode = WidgetBackgroundChoice.Dynamic.encodeMode()
        assertEquals(WidgetBackgroundChoice.MODE_DYNAMIC, mode)
        assertEquals(WidgetBackgroundChoice.Dynamic, decodeBackgroundChoice(mode, color = null))
    }

    @Test
    fun `theme encodes mode and decodes back`() {
        val mode = WidgetBackgroundChoice.Theme.encodeMode()
        assertEquals(WidgetBackgroundChoice.MODE_THEME, mode)
        assertEquals(WidgetBackgroundChoice.Theme, decodeBackgroundChoice(mode, color = null))
    }

    @Test
    fun `solid round-trips exact ARGB`() {
        val red = WidgetBackgroundChoice.Solid(Color.RED)
        val mode = red.encodeMode()
        assertEquals(WidgetBackgroundChoice.MODE_SOLID, mode)
        assertEquals(red, decodeBackgroundChoice(mode, color = Color.RED))
    }

    @Test
    fun `solid mode without color falls back to dynamic`() {
        // Defensive: if storage is corrupted (mode=Solid but color absent), don't crash.
        assertEquals(
            WidgetBackgroundChoice.Dynamic,
            decodeBackgroundChoice(WidgetBackgroundChoice.MODE_SOLID, color = null),
        )
    }

    @Test
    fun `unknown mode decodes to dynamic`() {
        assertEquals(WidgetBackgroundChoice.Dynamic, decodeBackgroundChoice(mode = null, color = null))
        assertEquals(WidgetBackgroundChoice.Dynamic, decodeBackgroundChoice(mode = 99, color = null))
    }

    @Test
    fun `presets contain six entries with full alpha`() {
        assertEquals(6, WidgetBackgroundChoice.PRESETS.size)
        WidgetBackgroundChoice.PRESETS.forEach { argb ->
            assertEquals("Preset must have alpha 0xFF", 0xFF, Color.alpha(argb))
        }
    }
}
