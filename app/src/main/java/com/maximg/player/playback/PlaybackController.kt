package com.maximg.player.playback

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.maximg.player.data.PlaylistRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class PlaybackController(
    private val context: Context,
    private val playlistRepository: PlaylistRepository,
    private val playbackStateStore: PlaybackStateStore
) {
    enum class TogglePlayPauseResult {
        PLAYING,
        PAUSED,
        NEEDS_QUEUE,
        FAILED
    }

    enum class WidgetLoopMode(
        val shuffleEnabled: Boolean,
        val repeatMode: Int
    ) {
        NO_REPEAT(
            shuffleEnabled = false,
            repeatMode = Player.REPEAT_MODE_OFF
        ),
        REPEAT_PLAYLIST(
            shuffleEnabled = false,
            repeatMode = Player.REPEAT_MODE_ALL
        ),
        REPEAT_TRACK(
            shuffleEnabled = false,
            repeatMode = Player.REPEAT_MODE_ONE
        ),
        SHUFFLE(
            shuffleEnabled = true,
            repeatMode = Player.REPEAT_MODE_OFF
        );

        fun next(): WidgetLoopMode {
            val modes = entries
            return modes[(ordinal + 1) % modes.size]
        }

        companion object {
            fun fromState(shuffleEnabled: Boolean, repeatMode: Int): WidgetLoopMode {
                if (shuffleEnabled) return SHUFFLE
                return when (repeatMode) {
                    Player.REPEAT_MODE_ALL -> REPEAT_PLAYLIST
                    Player.REPEAT_MODE_ONE -> REPEAT_TRACK
                    else -> NO_REPEAT
                }
            }
        }
    }

    suspend fun playPlaylist(playlistId: Long, startIndex: Int = 0, shuffle: Boolean? = null) {
        val tracks = playlistRepository.getPlaylistTracksOnce(playlistId)
        if (tracks.isEmpty()) return

        val state = playbackStateStore.stateFlow.first()
        val shouldShuffle = shuffle ?: state.shuffle
        val orderedTracks = if (shouldShuffle) tracks.shuffled() else tracks
        val clampedStart = startIndex.coerceIn(0, orderedTracks.lastIndex)

        val items = orderedTracks.map { track ->
            MediaItem.Builder()
                .setUri(track.contentUri)
                .setMediaId(track.mediaStoreId.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .build()
                )
                .build()
        }

        val navigation = runCatching {
            withController { controller ->
                controller.setShuffleModeEnabled(shouldShuffle)
                controller.repeatMode = state.repeatMode
                controller.setMediaItems(items, clampedStart, 0)
                controller.prepare()
                controller.play()
                controller.hasPreviousMediaItem() to controller.hasNextMediaItem()
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to start playlist playback for playlistId=$playlistId", error)
        }.getOrNull() ?: return

        playbackStateStore.setPlaylistId(playlistId)
        playbackStateStore.setShuffle(shouldShuffle)
        playbackStateStore.setPlaying(true)
        playbackStateStore.setNavigation(
            canGoPrevious = navigation.first,
            canGoNext = navigation.second
        )
    }

    suspend fun togglePlayPause(currentlyPlaying: Boolean? = null): TogglePlayPauseResult {
        val toggleResult = runCatching {
            withController { controller ->
                val shouldPlay = !(currentlyPlaying ?: controller.isPlaying)
                if (shouldPlay) {
                    if (controller.mediaItemCount == 0) {
                        TogglePlayPauseResult.NEEDS_QUEUE
                    } else {
                        controller.play()
                        TogglePlayPauseResult.PLAYING
                    }
                } else {
                    controller.pause()
                    TogglePlayPauseResult.PAUSED
                }
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to toggle play/pause", error)
        }.getOrElse { TogglePlayPauseResult.FAILED }

        when (toggleResult) {
            TogglePlayPauseResult.PLAYING -> playbackStateStore.setPlaying(true)
            TogglePlayPauseResult.PAUSED -> playbackStateStore.setPlaying(false)
            TogglePlayPauseResult.NEEDS_QUEUE -> Unit
            TogglePlayPauseResult.FAILED -> Unit
        }
        return toggleResult
    }

    suspend fun next() {
        runCatching {
            withController { controller ->
                controller.seekToNext()
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to seek to next", error)
        }
    }

    suspend fun previous() {
        runCatching {
            withController { controller ->
                controller.seekToPrevious()
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to seek to previous", error)
        }
    }

    suspend fun setShuffle(enabled: Boolean) {
        val applied = runCatching {
            withController { controller ->
                controller.setShuffleModeEnabled(enabled)
            }
            true
        }.onFailure { error ->
            Log.e(TAG, "Failed to set shuffle=$enabled", error)
        }.getOrDefault(false)
        if (!applied) return
        playbackStateStore.setShuffle(enabled)
    }

    suspend fun setRepeat(mode: Int) {
        val applied = runCatching {
            withController { controller ->
                controller.repeatMode = mode
            }
            true
        }.onFailure { error ->
            Log.e(TAG, "Failed to set repeat mode=$mode", error)
        }.getOrDefault(false)
        if (!applied) return
        playbackStateStore.setRepeat(mode)
    }

    suspend fun hasMediaItems(): Boolean {
        return runCatching {
            withController { controller -> controller.mediaItemCount > 0 }
        }.onFailure { error ->
            Log.e(TAG, "Failed to read media queue", error)
        }.getOrDefault(false)
    }

    suspend fun canGoPrevious(): Boolean {
        return runCatching {
            withController { controller -> controller.hasPreviousMediaItem() }
        }.onFailure { error ->
            Log.e(TAG, "Failed to check previous availability", error)
        }.getOrDefault(false)
    }

    suspend fun canGoNext(): Boolean {
        return runCatching {
            withController { controller -> controller.hasNextMediaItem() }
        }.onFailure { error ->
            Log.e(TAG, "Failed to check next availability", error)
        }.getOrDefault(false)
    }

    suspend fun getWidgetLoopMode(): WidgetLoopMode {
        return runCatching {
            withController { controller ->
                WidgetLoopMode.fromState(
                    shuffleEnabled = controller.shuffleModeEnabled,
                    repeatMode = controller.repeatMode
                )
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to read widget loop mode from controller", error)
        }.getOrElse {
            val state = playbackStateStore.stateFlow.first()
            WidgetLoopMode.fromState(
                shuffleEnabled = state.shuffle,
                repeatMode = state.repeatMode
            )
        }
    }

    suspend fun applyWidgetLoopMode(mode: WidgetLoopMode) {
        val applied = runCatching {
            withController { controller ->
                controller.setShuffleModeEnabled(mode.shuffleEnabled)
                controller.repeatMode = mode.repeatMode
            }
            true
        }.onFailure { error ->
            Log.e(
                TAG,
                "Failed to apply widget loop mode (shuffle=${mode.shuffleEnabled}, repeat=${mode.repeatMode})",
                error
            )
        }.getOrDefault(false)
        if (!applied) return
        playbackStateStore.setLoopMode(
            shuffleEnabled = mode.shuffleEnabled,
            repeatMode = mode.repeatMode
        )
    }

    private suspend inline fun <T> withController(crossinline block: (MediaController) -> T): T {
        return withContext(Dispatchers.Main.immediate) {
            val firstAttempt = runCatching {
                block(MediaControllerProvider.get(context))
            }
            if (firstAttempt.isSuccess) {
                firstAttempt.getOrThrow()
            } else {
                MediaControllerProvider.release()
                block(MediaControllerProvider.get(context))
            }
        }
    }

    companion object {
        private const val TAG = "PlaybackController"

        fun cycleWidgetLoopMode(shuffleEnabled: Boolean, repeatMode: Int): WidgetLoopMode {
            return WidgetLoopMode.fromState(shuffleEnabled, repeatMode).next()
        }

        fun cycleRepeatMode(currentMode: Int): Int {
            return when (currentMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
}
