package dev.maxxximgb.genesis.data.playback

import androidx.media3.common.MediaItem
import dev.maxxximgb.genesis.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaItemMappingTest {

    private val track = Track(
        mediaStoreId = 42L,
        title = "Лесник",
        artist = "Король и Шут",
        album = "Будь как сома",
        albumId = 7L,
        durationMs = 240_000L,
        contentUri = "content://media/external/audio/media/42",
        dateAdded = 1_700_000_000L,
    )

    @Test
    fun mapsAllMetadataFields() {
        val item: MediaItem = track.toMediaItem(playlistId = 13L)

        assertEquals("42", item.mediaId)
        assertEquals("content://media/external/audio/media/42", item.localConfiguration?.uri.toString())
        val md = item.mediaMetadata
        assertEquals("Лесник", md.title)
        assertEquals("Король и Шут", md.artist)
        assertEquals("Будь как сома", md.albumTitle)
        assertNotNull(md.artworkUri)
        assertTrue(md.artworkUri.toString().endsWith("/7"))
    }

    @Test
    fun mediaStoreIdRoundtrip() {
        val item = track.toMediaItem()
        assertEquals(42L, item.mediaStoreId())
    }

    @Test
    fun playlistIdRoundtripWhenProvided() {
        val item = track.toMediaItem(playlistId = 99L)
        assertEquals(99L, item.playlistId())
    }

    @Test
    fun playlistIdAbsentWhenNotProvided() {
        val item = track.toMediaItem(playlistId = null)
        assertNull(item.playlistId())
    }

    @Test
    fun nullableMetadataFieldsTolerated() {
        val sparse = track.copy(artist = null, album = null, albumId = null)
        val item = sparse.toMediaItem()
        val md = item.mediaMetadata
        assertNull(md.artist)
        assertNull(md.albumTitle)
        assertNull(md.artworkUri)
    }
}
