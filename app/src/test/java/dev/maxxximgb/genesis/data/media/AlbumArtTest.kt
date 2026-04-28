package dev.maxxximgb.genesis.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlbumArtTest {

    @Test
    fun buildsContentUriForGivenAlbumId() {
        assertEquals(
            "content://media/external/audio/albumart/123",
            AlbumArt.uri(123L).toString(),
        )
    }

    @Test
    fun returnsNullForNullAlbumId() {
        assertNull(AlbumArt.uri(null))
    }
}
