package dev.maxxximgb.genesis.domain.model

data class PlaybackState(
    val isPlaying: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val currentMediaStoreId: Long? = null,
    val playlistId: Long? = null,
    val queue: List<Long> = emptyList(),
    val currentIndex: Int = -1,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    /**
     * Player-MediaItem-indices in actual play order, starting with the currently playing
     * track at position 0. With shuffle off this is just `[currentIndex, currentIndex + 1, ...]`;
     * with shuffle on it follows the shuffled walk through the timeline. The Up Next panel
     * iterates this list (skipping element 0) to render upcoming tracks in the order they'll
     * actually play.
     */
    val playOrderIndices: List<Int> = emptyList(),
    /**
     * Per-item source metadata (playlist / album the item was enqueued from), aligned 1:1
     * with [queue] by index. Drives the queue panel's "group by source" UI.
     */
    val queueEntries: List<QueueEntry> = emptyList(),
)
