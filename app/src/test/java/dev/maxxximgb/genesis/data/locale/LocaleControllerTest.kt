package dev.maxxximgb.genesis.data.locale

import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleControllerTest {

    @Test
    fun fromTagMatchesEnumValues() {
        assertEquals(LocaleController.Language.SYSTEM, LocaleController.Language.fromTag(null))
        assertEquals(LocaleController.Language.SYSTEM, LocaleController.Language.fromTag(""))
        assertEquals(LocaleController.Language.SYSTEM, LocaleController.Language.fromTag("   "))
        assertEquals(LocaleController.Language.ENGLISH, LocaleController.Language.fromTag("en"))
        assertEquals(LocaleController.Language.ENGLISH, LocaleController.Language.fromTag("en-US"))
        assertEquals(LocaleController.Language.ENGLISH, LocaleController.Language.fromTag("en-GB"))
        assertEquals(LocaleController.Language.RUSSIAN, LocaleController.Language.fromTag("ru"))
        assertEquals(LocaleController.Language.RUSSIAN, LocaleController.Language.fromTag("ru-RU"))
        assertEquals(LocaleController.Language.SPANISH, LocaleController.Language.fromTag("es"))
        assertEquals(LocaleController.Language.SPANISH, LocaleController.Language.fromTag("es-ES"))
        assertEquals(LocaleController.Language.CHINESE, LocaleController.Language.fromTag("zh"))
        assertEquals(LocaleController.Language.CHINESE, LocaleController.Language.fromTag("zh-CN"))
        assertEquals(LocaleController.Language.CHINESE, LocaleController.Language.fromTag("zh-TW"))
        assertEquals(LocaleController.Language.GERMAN, LocaleController.Language.fromTag("de"))
        assertEquals(LocaleController.Language.GERMAN, LocaleController.Language.fromTag("de-DE"))
        assertEquals(LocaleController.Language.FRENCH, LocaleController.Language.fromTag("fr"))
        assertEquals(LocaleController.Language.PORTUGUESE, LocaleController.Language.fromTag("pt"))
        assertEquals(LocaleController.Language.PORTUGUESE, LocaleController.Language.fromTag("pt-BR"))
        assertEquals(LocaleController.Language.JAPANESE, LocaleController.Language.fromTag("ja"))
        // Unknown locale still falls back to SYSTEM
        assertEquals(LocaleController.Language.SYSTEM, LocaleController.Language.fromTag("ko"))
        assertEquals(LocaleController.Language.SYSTEM, LocaleController.Language.fromTag("xx"))
    }

    @Test
    fun systemLanguageHasNullTag() {
        assertEquals(null, LocaleController.Language.SYSTEM.tag)
    }

    @Test
    fun languageTagsMatchBcp47() {
        assertEquals("en", LocaleController.Language.ENGLISH.tag)
        assertEquals("ru", LocaleController.Language.RUSSIAN.tag)
        assertEquals("es", LocaleController.Language.SPANISH.tag)
        assertEquals("zh", LocaleController.Language.CHINESE.tag)
        assertEquals("de", LocaleController.Language.GERMAN.tag)
        assertEquals("fr", LocaleController.Language.FRENCH.tag)
        assertEquals("pt", LocaleController.Language.PORTUGUESE.tag)
        assertEquals("ja", LocaleController.Language.JAPANESE.tag)
    }
}
