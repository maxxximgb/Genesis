package dev.maxxximgb.genesis.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBackgroundChoiceTest {

    @Test
    fun `dark encodes mode and decodes back`() {
        val mode = WidgetBackgroundChoice.Dark.encodeMode()
        assertEquals(WidgetBackgroundChoice.MODE_DARK, mode)
        assertEquals(WidgetBackgroundChoice.Dark, decodeBackgroundChoice(mode))
    }

    @Test
    fun `light encodes mode and decodes back`() {
        val mode = WidgetBackgroundChoice.Light.encodeMode()
        assertEquals(WidgetBackgroundChoice.MODE_LIGHT, mode)
        assertEquals(WidgetBackgroundChoice.Light, decodeBackgroundChoice(mode))
    }

    @Test
    fun `unknown or missing mode decodes to null`() {
        assertNull(decodeBackgroundChoice(mode = null))
        // Old Solid mode (2) from the previous schema is no longer recognized.
        assertNull(decodeBackgroundChoice(mode = 2))
        assertNull(decodeBackgroundChoice(mode = 99))
    }

    @Test
    fun `colorsFor produces white-on-dark for Dark and dark-on-light for Light`() {
        val dark = WidgetBackgroundChoice.colorsFor(WidgetBackgroundChoice.Dark)
        val light = WidgetBackgroundChoice.colorsFor(WidgetBackgroundChoice.Light)
        // Dark background luminance < light background luminance.
        assertTrue("Dark bg must be darker than Light bg",
            (dark.background and 0xFF) < (light.background and 0xFF))
        // onBackground colors flip accordingly.
        assertTrue("Dark onBackground must be brighter than its background",
            (dark.onBackground and 0xFF) > (dark.background and 0xFF))
        assertTrue("Light onBackground must be darker than its background",
            (light.onBackground and 0xFF) < (light.background and 0xFF))
    }
}
