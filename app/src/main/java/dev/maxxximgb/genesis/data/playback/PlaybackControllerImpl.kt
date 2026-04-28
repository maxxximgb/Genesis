package dev.maxxximgb.genesis.data.playback

import androidx.media3.session.MediaController
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.RepeatMode
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackControllerImpl @Inject constructor(
    private val provider: MediaControllerProvider,
    store: PlaybackStateStore,
) : PlaybackController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate

    override val state: StateFlow<PlaybackState> = store.flow.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS),
        initialValue = PlaybackState(),
    )

    override suspend fun playSingle(track: Track) {
        onMain { c ->
            c.setMediaItems(listOf(track.toMediaItem()))
            c.prepare()
            c.play()
        }
    }

    override suspend fun playQueue(playlistId: Long?, tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        onMain { c ->
            c.setMediaItems(tracks.map { it.toMediaItem(playlistId) }, safeIndex, 0L)
            c.prepare()
            c.play()
        }
    }

    override suspend fun togglePlayPause() {
        onMain { c ->
            if (c.isPlaying) c.pause() else c.play()
        }
    }

    override suspend fun seekToNext() {
        onMain { it.seekToNext() }
    }

    override suspend fun seekToPrevious() {
        onMain { it.seekToPrevious() }
    }

    override suspend fun seekTo(positionMs: Long) {
        onMain { it.seekTo(positionMs) }
    }

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        onMain { it.shuffleModeEnabled = enabled }
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        onMain { it.repeatMode = mode.toPlayer() }
    }

    private suspend inline fun onMain(crossinline block: (MediaController) -> Unit) {
        val controller = provider.get()
        withContext(mainDispatcher) { block(controller) }
    }

    private companion object {
        const val STATE_TIMEOUT_MS = 5_000L
    }
}
