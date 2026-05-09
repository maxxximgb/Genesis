package dev.maxxximgb.genesis.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class PaletteResolverTest {

    @Test
    fun fromNameMatchesEnumNamesCaseSensitively() {
        assertEquals(PaletteId.ITUNES, PaletteId.fromName("ITUNES"))
    }

    @Test
    fun unknownNameFallsBackToDefault() {
        assertEquals(PaletteId.ITUNES, PaletteId.fromName("WTF"))
        assertEquals(PaletteId.ITUNES, PaletteId.fromName(null))
        assertEquals(PaletteId.ITUNES, PaletteId.fromName(""))
        assertEquals(PaletteId.ITUNES, PaletteId.fromName("itunes")) // wrong case
    }

    @Test
    fun defaultIsItunes() {
        assertEquals(PaletteId.ITUNES, PaletteId.DEFAULT)
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
