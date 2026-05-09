package dev.maxxximgb.genesis.domain.playback

import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.RepeatMode
import dev.maxxximgb.genesis.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {

    val state: StateFlow<PlaybackState>

    suspend fun playSingle(track: Track)

    suspend fun playQueue(
        playlistId: Long?,
        tracks: List<Track>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L,
    )

    suspend fun togglePlayPause()

    suspend fun play()

    suspend fun pause()

    suspend fun seekToNext()

    suspend fun seekToPrevious()

    suspend fun seekTo(positionMs: Long)

    suspend fun seekToQueueIndex(index: Int)

    suspend fun setShuffleEnabled(enabled: Boolean)

    /**
     * Re-randomize the shuffle order while shuffle is on. Implemented as a quick toggle
     * (off, then on) — the player's shuffle order is regenerated on the second enable, so
     * the upcoming queue gets a fresh permutation without disrupting the current track.
     * Caller should ensure shuffle is currently on; otherwise this is a no-op.
     */
    suspend fun reshuffleQueue()

    suspend fun setRepeatMode(mode: RepeatMode)

    /**
     * Append [tracks] to the very end of the current queue without disturbing playback.
     * [sourcePlaylistId] / [sourceAlbumId] tag each appended item so the queue UI can
     * group them under a "Playlist X" / "Album Y" header. Pass null for ad-hoc adds
     * (single-track enqueues, etc.) — those entries become "orphans" and are hidden.
     */
    suspend fun enqueueAtEnd(
        tracks: List<Track>,
        sourcePlaylistId: Long? = null,
        sourceAlbumId: Long? = null,
    )

    /** Insert [tracks] right after the currently-playing index — they play next. */
    suspend fun enqueueAfterCurrent(
        tracks: List<Track>,
        sourcePlaylistId: Long? = null,
        sourceAlbumId: Long? = null,
    )

    /**
     * Reorder the queue: move the item at [fromIndex] to [toIndex]. Indices refer to
     * the underlying player timeline (i.e., positions in `state.queue`), not the
     * shuffle-aware play order. Caller is responsible for translating any UI-visible
     * "next-in-playback" index back to the timeline index.
     */
    suspend fun moveQueueItem(fromIndex: Int, toIndex: Int)

    /**
     * Remove every queue item whose mediaStoreId equals [mediaStoreId]. Called from the
     * track-delete flow so a freshly deleted track doesn't keep showing up in Up Next
     * (and, more critically, doesn't try to play from a URI whose backing file the
     * system just removed). If the item being removed is the currently-playing one,
     * Media3 advances to the next track automatically; if the queue empties, the
     * player stops.
     */
    suspend fun removeFromQueue(mediaStoreId: Long)

    /**
     * Update the displayed title of every queue item whose mediaStoreId equals
     * [mediaStoreId]. Called from the rename flow so Up Next, Now Playing, and the
     * widget pick up the new name immediately instead of waiting for a re-enqueue.
     * URI / MediaId stay identical so playback continues uninterrupted.
     */
    suspend fun updateQueueItemTitle(mediaStoreId: Long, newTitle: String)
}
