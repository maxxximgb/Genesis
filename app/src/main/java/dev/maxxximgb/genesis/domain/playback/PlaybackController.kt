package dev.maxxximgb.genesis.domain.playback

import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.RepeatMode
import dev.maxxximgb.genesis.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {

    val state: StateFlow<PlaybackState>

    suspend fun playSingle(track: Track)

    suspend fun playQueue(playlistId: Long?, tracks: List<Track>, startIndex: Int = 0)

    suspend fun togglePlayPause()

    suspend fun play()

    suspend fun pause()

    suspend fun seekToNext()

    suspend fun seekToPrevious()

    suspend fun seekTo(positionMs: Long)

    suspend fun setShuffleEnabled(enabled: Boolean)

    suspend fun setRepeatMode(mode: RepeatMode)
}
