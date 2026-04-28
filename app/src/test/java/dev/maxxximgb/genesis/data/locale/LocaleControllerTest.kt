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
        // Unknown locale falls back to SYSTEM (handled by Android's localeConfig at runtime)
        assertEquals(LocaleController.Language.SYSTEM, LocaleController.Language.fromTag("de"))
        assertEquals(LocaleController.Language.SYSTEM, LocaleController.Language.fromTag("zh-CN"))
    }

    @Test
    fun systemLanguageHasNullTag() {
        assertEquals(null, LocaleController.Language.SYSTEM.tag)
    }

    @Test
    fun englishAndRussianTagsMatchBcp47() {
        assertEquals("en", LocaleController.Language.ENGLISH.tag)
        assertEquals("ru", LocaleController.Language.RUSSIAN.tag)
    }
}
