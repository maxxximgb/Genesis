package dev.maxxximgb.genesis.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class PaletteResolverTest {

    @Test
    fun fromNameMatchesEnumNamesCaseSensitively() {
        assertEquals(PaletteId.VERDANT, PaletteId.fromName("VERDANT"))
        assertEquals(PaletteId.AMBER, PaletteId.fromName("AMBER"))
        assertEquals(PaletteId.INDIGO, PaletteId.fromName("INDIGO"))
        assertEquals(PaletteId.CRIMSON, PaletteId.fromName("CRIMSON"))
        assertEquals(PaletteId.TEAL, PaletteId.fromName("TEAL"))
        assertEquals(PaletteId.DYNAMIC, PaletteId.fromName("DYNAMIC"))
    }

    @Test
    fun unknownNameFallsBackToVerdant() {
        assertEquals(PaletteId.VERDANT, PaletteId.fromName("WTF"))
        assertEquals(PaletteId.VERDANT, PaletteId.fromName(null))
        assertEquals(PaletteId.VERDANT, PaletteId.fromName(""))
        assertEquals(PaletteId.VERDANT, PaletteId.fromName("verdant")) // wrong case
    }

    @Test
    fun defaultIsVerdant() {
        assertEquals(PaletteId.VERDANT, PaletteId.DEFAULT)
    }

    @Test
    fun allPalettesHaveSeedAndDisplayResource() {
        for (palette in PaletteId.entries) {
            assertEquals(true, palette.seed.alpha > 0f) // seed is fully opaque
            assertEquals(true, palette.displayResId != 0)
        }
    }

    @Test
    fun themeModeFromNameMatchesEnumNames() {
        assertEquals(ThemeMode.AUTO, ThemeMode.fromName("AUTO"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromName("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromName("DARK"))
        assertEquals(ThemeMode.AUTO, ThemeMode.fromName("WTF"))
        assertEquals(ThemeMode.AUTO, ThemeMode.fromName(null))
    }

    @Test
    fun themeModeDefaultIsAuto() {
        assertEquals(ThemeMode.AUTO, ThemeMode.DEFAULT)
    }
}
