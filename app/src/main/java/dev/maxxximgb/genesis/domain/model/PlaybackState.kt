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
)
