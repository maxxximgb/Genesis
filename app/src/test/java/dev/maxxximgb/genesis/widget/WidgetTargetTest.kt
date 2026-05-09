package dev.maxxximgb.genesis.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetTargetTest {

    @Test
    fun audiobooksRoundTrips() {
        val encoded = WidgetTarget.Audiobooks.encode()
        assertEquals(WIDGET_TARGET_AUDIOBOOKS, encoded)
        assertEquals(WidgetTarget.Audiobooks, decodeWidgetTarget(encoded))
    }

    @Test
    fun playlistRoundTrips() {
        val encoded = WidgetTarget.Playlist(42L).encode()
        assertEquals("playlist:42", encoded)
        assertEquals(WidgetTarget.Playlist(42L), decodeWidgetTarget(encoded))
    }

    @Test
    fun decodeRejectsGarbage() {
        assertNull(decodeWidgetTarget(null))
        assertNull(decodeWidgetTarget(""))
        assertNull(decodeWidgetTarget("not-a-target"))
        assertNull(decodeWidgetTarget("playlist:not-a-number"))
    }

    @Test
    fun pseudoIdSentinelDetection() {
        assertEquals(true, AUDIOBOOK_PSEUDO_PLAYLIST_ID.isAudiobookTarget())
        assertEquals(false, 0L.isAudiobookTarget())
        assertEquals(false, (null as Long?).isAudiobookTarget())
    }
}
