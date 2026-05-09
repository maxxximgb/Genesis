package dev.maxxximgb.genesis.data.playback

import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
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

/**
 * Custom MediaSession command that asks PlayerService to regenerate the player's ShuffleOrder.
 * Toggling [Player.setShuffleModeEnabled] only flips the flag and does NOT re-randomize the
 * order in Media3 — for that you need [ExoPlayer.setShuffleOrder], which lives on the
 * concrete ExoPlayer instance (not on MediaController). This action bridges that gap.
 */
internal const val ACTION_RESHUFFLE = "dev.maxxximgb.genesis.action.RESHUFFLE"

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

    override suspend fun playQueue(
        playlistId: Long?,
        tracks: List<Track>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        onMain { c ->
            c.setMediaItems(
                // Tag every item with sourcePlaylistId = playlistId so the queue UI's
                // group-by-source step recognises this as a coherent batch instead of
                // a wall of orphan tracks. The whole queue is the playlist; that's its
                // source.
                tracks.map { it.toMediaItem(playlistId, sourcePlaylistId = playlistId) },
                safeIndex,
                startPositionMs.coerceAtLeast(0L),
            )
            c.prepare()
            c.play()
        }
    }

    override suspend fun togglePlayPause() {
        onMain { c ->
            if (c.isPlaying) c.pause() else c.play()
        }
    }

    override suspend fun play() {
        onMain { it.play() }
    }

    override suspend fun pause() {
        onMain { it.pause() }
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

    override suspend fun seekToQueueIndex(index: Int) {
        onMain { it.seekTo(index, 0L) }
    }

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        onMain { it.shuffleModeEnabled = enabled }
    }

    override suspend fun reshuffleQueue() {
        onMain { c ->
            if (!c.shuffleModeEnabled) return@onMain
            c.sendCustomCommand(SessionCommand(ACTION_RESHUFFLE, Bundle.EMPTY), Bundle.EMPTY)
        }
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        onMain { it.repeatMode = mode.toPlayer() }
    }

    override suspend fun enqueueAtEnd(
        tracks: List<Track>,
        sourcePlaylistId: Long?,
        sourceAlbumId: Long?,
    ) {
        if (tracks.isEmpty()) return
        // Inherit the current queue's playlistId so the appended items belong to the same
        // logical context (matters for the bookmark-by-playlistId persistence path). The
        // per-item source ids are independent — they tag where this batch was added FROM,
        // for the queue group-by-source UI.
        val playlistId = state.value.playlistId
        onMain { c ->
            c.addMediaItems(
                tracks.map {
                    it.toMediaItem(
                        playlistId = playlistId,
                        sourcePlaylistId = sourcePlaylistId,
                        sourceAlbumId = sourceAlbumId,
                    )
                },
            )
        }
    }

    override suspend fun enqueueAfterCurrent(
        tracks: List<Track>,
        sourcePlaylistId: Long?,
        sourceAlbumId: Long?,
    ) {
        if (tracks.isEmpty()) return
        val playlistId = state.value.playlistId
        onMain { c ->
            val insertAt = (c.currentMediaItemIndex + 1).coerceAtLeast(0)
            c.addMediaItems(
                insertAt,
                tracks.map {
                    it.toMediaItem(
                        playlistId = playlistId,
                        sourcePlaylistId = sourcePlaylistId,
                        sourceAlbumId = sourceAlbumId,
                    )
                },
            )
        }
    }

    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        onMain { c ->
            val count = c.mediaItemCount
            if (fromIndex < 0 || fromIndex >= count) return@onMain
            val clampedTo = toIndex.coerceIn(0, count - 1)
            c.moveMediaItem(fromIndex, clampedTo)
        }
    }

    override suspend fun removeFromQueue(mediaStoreId: Long) {
        onMain { c ->
            // Walk back-to-front: removing earlier indices would shift later ones,
            // so iterating in reverse keeps the lookups stable. Hitting the
            // currently-playing index is fine — Media3 auto-advances; if the queue
            // becomes empty the player stops cleanly.
            for (i in c.mediaItemCount - 1 downTo 0) {
                if (c.getMediaItemAt(i).mediaStoreId() == mediaStoreId) {
                    c.removeMediaItem(i)
                }
            }
        }
    }

    override suspend fun updateQueueItemTitle(mediaStoreId: Long, newTitle: String) {
        onMain { c ->
            for (i in 0 until c.mediaItemCount) {
                val existing = c.getMediaItemAt(i)
                if (existing.mediaStoreId() != mediaStoreId) continue
                // Build a fresh MediaItem that reuses every field but swaps the title.
                // MediaId / URI / extras stay identical so playback isn't restarted —
                // Media3 treats this as a metadata-only patch.
                val newMetadata = existing.mediaMetadata.buildUpon()
                    .setTitle(newTitle)
                    .build()
                val replacement = existing.buildUpon()
                    .setMediaMetadata(newMetadata)
                    .build()
                c.replaceMediaItem(i, replacement)
            }
        }
    }

    private suspend inline fun onMain(crossinline block: (MediaController) -> Unit) {
        val controller = provider.get()
        withContext(mainDispatcher) { block(controller) }
    }

    private companion object {
        const val STATE_TIMEOUT_MS = 5_000L
    }
}
