package dev.maxxximgb.genesis.widget

import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.PlaylistDetail
import dev.maxxximgb.genesis.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetRenderModeTest {

    private fun track(id: Long) = Track(
        mediaStoreId = id,
        title = "T$id",
        artist = null,
        album = null,
        albumId = null,
        durationMs = 1L,
        contentUri = "u://$id",
        dateAdded = id,
    )

    private fun detail(id: Long, count: Int): PlaylistDetail = PlaylistDetail(
        playlist = Playlist(id = id, name = "P$id", createdAt = 0L),
        tracks = (1..count).map { track(it.toLong()) },
    )

    @Test
    fun unboundWhenNoPlaylistId() {
        val mode = widgetRenderMode(
            widgetPlaylistId = null,
            detail = null,
            state = PlaybackState(),
        )
        assertEquals(WidgetRenderMode.UNBOUND, mode)
    }

    @Test
    fun missingPlaylistWhenDetailIsNull() {
        val mode = widgetRenderMode(
            widgetPlaylistId = 1L,
            detail = null,
            state = PlaybackState(),
        )
        assertEquals(WidgetRenderMode.MISSING_PLAYLIST, mode)
    }

    @Test
    fun emptyPlaylistWhenNoTracks() {
        val mode = widgetRenderMode(
            widgetPlaylistId = 1L,
            detail = detail(1L, 0),
            state = PlaybackState(),
        )
        assertEquals(WidgetRenderMode.EMPTY_PLAYLIST, mode)
    }

    @Test
    fun foreignWhenDifferentPlaylistPlaying() {
        val mode = widgetRenderMode(
            widgetPlaylistId = 1L,
            detail = detail(1L, 3),
            state = PlaybackState(playlistId = 2L, isPlaying = true),
        )
        assertEquals(WidgetRenderMode.FOREIGN, mode)
    }

    @Test
    fun foreignWhenNothingPlaying() {
        val mode = widgetRenderMode(
            widgetPlaylistId = 1L,
            detail = detail(1L, 3),
            state = PlaybackState(playlistId = null),
        )
        assertEquals(WidgetRenderMode.FOREIGN, mode)
    }

    @Test
    fun ownPlayingWhenSamePlaylistAndIsPlaying() {
        val mode = widgetRenderMode(
            widgetPlaylistId = 1L,
            detail = detail(1L, 3),
            state = PlaybackState(playlistId = 1L, isPlaying = true),
        )
        assertEquals(WidgetRenderMode.OWN_PLAYING, mode)
    }

    @Test
    fun ownPausedWhenSamePlaylistAndPaused() {
        val mode = widgetRenderMode(
            widgetPlaylistId = 1L,
            detail = detail(1L, 3),
            state = PlaybackState(playlistId = 1L, isPlaying = false),
        )
        assertEquals(WidgetRenderMode.OWN_PAUSED, mode)
    }
}
