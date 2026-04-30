package dev.maxxximgb.genesis.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackSubtitleTest {

    @Test fun allFieldsPresent() {
        assertEquals("Король и Шут · Лесник · 3:42", trackSubtitle("Король и Шут", "Лесник", 3 * 60_000L + 42_000L))
    }

    @Test fun missingArtist() {
        assertEquals("Лесник · 3:42", trackSubtitle(null, "Лесник", 222_000L))
    }

    @Test fun missingAlbum() {
        assertEquals("Король и Шут · 3:42", trackSubtitle("Король и Шут", null, 222_000L))
    }

    @Test fun blankAlbumTreatedAsMissing() {
        assertEquals("Король и Шут · 3:42", trackSubtitle("Король и Шут", "   ", 222_000L))
    }

    @Test fun onlyDuration() {
        assertEquals("3:42", trackSubtitle(null, null, 222_000L))
    }

    @Test fun zeroDurationOmitted() {
        assertEquals("Король и Шут · Лесник", trackSubtitle("Король и Шут", "Лесник", 0L))
    }

    @Test fun negativeDurationOmitted() {
        assertEquals("Король и Шут", trackSubtitle("Король и Шут", null, -1L))
    }

    @Test fun allEmptyReturnsDash() {
        assertEquals("—", trackSubtitle(null, "", 0L))
    }

    @Test fun trimsWhitespace() {
        assertEquals("Король · Лесник · 1:00", trackSubtitle("  Король  ", " Лесник ", 60_000L))
    }
}
