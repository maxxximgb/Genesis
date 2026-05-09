package dev.maxxximgb.genesis.service

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import dev.maxxximgb.genesis.data.audiofx.AudioFxController
import dev.maxxximgb.genesis.data.playback.ACTION_RESHUFFLE
import dev.maxxximgb.genesis.data.playback.PlaybackStateStore
import dev.maxxximgb.genesis.data.playback.mediaStoreId
import dev.maxxximgb.genesis.data.playback.playlistId
import dev.maxxximgb.genesis.data.playback.sourceAlbumId
import dev.maxxximgb.genesis.data.playback.sourcePlaylistId
import dev.maxxximgb.genesis.data.preferences.BookmarkStore
import dev.maxxximgb.genesis.data.preferences.PlaylistModeStore
import dev.maxxximgb.genesis.data.playback.toMediaItem
import dev.maxxximgb.genesis.data.playback.toPlayer
import dev.maxxximgb.genesis.data.playback.toRepeatMode
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.loopStateOf
import dev.maxxximgb.genesis.domain.model.toRepeatAndShuffle
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PlayerService : MediaSessionService() {

    @Inject
    lateinit var stateStore: PlaybackStateStore

    @Inject
    lateinit var libraryRepository: MediaLibraryRepository

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    @Inject
    lateinit var bookmarkStore: BookmarkStore

    @Inject
    lateinit var playlistModeStore: PlaylistModeStore

    @Inject
    lateinit var audioFxController: AudioFxController

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionTickerJob: Job? = null

    // Last (playlistId, mediaStoreId, positionMs) we wrote to BookmarkStore. Lets writeBookmark
    // skip a redundant DataStore edit when several player callbacks fire back-to-back at the
    // same position (e.g., pause → onIsPlayingChanged + onPositionDiscontinuity).
    private var lastBookmarkWritten: Triple<Long?, Long, Long>? = null

    // Cached play-order walk; invalidated on the three callbacks that can change it
    // (timeline, shuffle, item transition). Avoids re-walking the timeline on every
    // writeState — onIsPlayingChanged/onRepeatModeChanged/seek discontinuities don't
    // change order, but each was rebuilding the full O(n) list.
    private var cachedPlayOrder: List<Int> = emptyList()
    private var playOrderDirty: Boolean = true

    private fun invalidatePlayOrder() { playOrderDirty = true }

    override fun onCreate() {
        super.onCreate()

        // Pre-allocate the audio session id and pin it to the player. Without this, the player
        // generates its own session id internally on the first prepare() and only publishes
        // it via onAudioSessionIdChanged. On some OEM stacks (Samsung Sound Alive in
        // particular) that callback never fires for our analytics listener, leaving the
        // AudioFxController unable to ever attach Equalizer/BassBoost/Virtualizer to the
        // active audio session — so EQ tweaks would be silently ignored at the HAL.
        // Generating the id ourselves and calling setAudioSessionId/attach immediately
        // guarantees the effect chain is bound to the correct session before any track loads.
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val preAllocatedSessionId = audioManager
            ?.let { runCatching { it.generateAudioSessionId() }.getOrNull() }
            ?.takeIf { it != AudioManager.ERROR && it != 0 }
            ?: 0

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setMaxSeekToPreviousPositionMs(SEEK_TO_PREVIOUS_THRESHOLD_MS)
            .build()

        if (preAllocatedSessionId != 0) {
            player.audioSessionId = preAllocatedSessionId
            audioFxController.attach(preAllocatedSessionId)
            Log.i(TAG, "Pre-allocated audio session id $preAllocatedSessionId, attached AudioFxController")
        }

        session = MediaSession.Builder(this, player)
            .setCallback(ReshuffleCallback())
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build()
        )

        // Backup path: if the player ever reassigns its audio session id at runtime (rare —
        // can happen on audio config changes), re-attach to the new id.
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int,
            ) {
                if (audioSessionId != 0 && audioSessionId != preAllocatedSessionId) {
                    Log.i(TAG, "Player session id changed to $audioSessionId, re-attaching")
                    audioFxController.reattach(audioSessionId)
                }
            }
        })

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                writeState(updateBookmark = true)
                if (isPlaying) startPositionTicker() else stopPositionTicker()
            }

            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                invalidatePlayOrder()
                writeState(updateBookmark = true)
                // Track changes can trigger ExoPlayer to recreate its internal AudioTrack
                // (different sample rate / channel layout). Some HALs lose effect parameters
                // across that recreation, manifesting as brief noise/hiss with stale-looking
                // band levels. Re-asserting the EQ state on every transition keeps the
                // Equalizer pinned to the live AudioTrack without releasing it.
                audioFxController.reapplyState()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                if (shuffleModeEnabled) regenerateShuffleOrderCurrentFirst()
                invalidatePlayOrder()
                writeState()
                syncModeStoreFromPlayer()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                writeState()
                syncModeStoreFromPlayer()
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                invalidatePlayOrder()
                writeState()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                // Push new position to DataStore immediately on seek so observers (Now Playing
                // seek bar, widget) reflect the jump without waiting up to POSITION_TICK_MS.
                // Auto-discontinuities (track transitions) are already handled by onMediaItemTransition.
                if (reason == Player.DISCONTINUITY_REASON_SEEK ||
                    reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
                ) {
                    writeState(updateBookmark = true)
                }
            }
        })

        scope.launch { restoreQueueFromStore() }
        scope.launch { observeAndApplyPlaylistMode() }
    }

    /**
     * Watches the active playlistId in [stateStore] and, for that playlist, observes the
     * authoritative loop mode in [playlistModeStore]. Any change applies to the live player
     * — this is the in-app counterpart to widget actions: when the user flips the mode from
     * the widget (which only writes to [playlistModeStore]), the running service picks the
     * change up here and reflects it onto the player. The equality guards keep the listener
     * from re-firing when our own write happens to match the current player flag (which
     * would be a no-op anyway, but the listener triggers a writeState round-trip we don't
     * need).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observeAndApplyPlaylistMode() {
        stateStore.flow
            .map { it.playlistId }
            .distinctUntilChanged()
            .flatMapLatest { pid ->
                if (pid == null) emptyFlow()
                else playlistModeStore.observeMode(pid)
            }
            .distinctUntilChanged()
            .collect { mode ->
                val (repeat, shuffle) = mode.toRepeatAndShuffle()
                val targetRepeat = repeat.toPlayer()
                withContext(Dispatchers.Main.immediate) {
                    if (player.shuffleModeEnabled != shuffle) {
                        player.shuffleModeEnabled = shuffle
                    }
                    if (player.repeatMode != targetRepeat) {
                        player.repeatMode = targetRepeat
                    }
                }
            }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = session

    override fun onDestroy() {
        stopPositionTicker()
        scope.cancel()
        audioFxController.release()
        session.release()
        player.release()
        super.onDestroy()
    }

    private fun writeState(updateBookmark: Boolean = false) {
        scope.launch {
            stateStore.update(snapshotState())
            if (updateBookmark) writeBookmark()
        }
        widgetUpdater.requestUpdate()
    }

    /**
     * Mirrors the current player's shuffle/repeat back to [PlaylistModeStore] for the active
     * playlistId. This closes the loop for *external* mode changes — anything that flips the
     * Player flags without going through [SetPlaylistLoopModeUseCase] (e.g., a future media
     * notification toggle, a Bluetooth headset gesture, an Android Auto control surface). Our
     * widget reads only [PlaylistModeStore]; without this write the widget would render the
     * pre-external-change mode until something else nudged the store.
     *
     * Pairs with [observeAndApplyPlaylistMode]'s equality guards: re-applying a value that
     * matches the current Player flag is a no-op, so the round-trip terminates after one hop.
     */
    private fun syncModeStoreFromPlayer() {
        val pid = player.currentMediaItem?.playlistId() ?: return
        val mode = loopStateOf(player.repeatMode.toRepeatMode(), player.shuffleModeEnabled)
        scope.launch { playlistModeStore.setMode(pid, mode) }
    }

    /**
     * Per-context (library / each playlist) resume bookmark. Updated on every state change
     * AND every position tick so the next launch can pick up exactly where we left off,
     * even mid-track.
     */
    private suspend fun writeBookmark() {
        val current = player.currentMediaItem ?: return
        val mediaStoreId = current.mediaStoreId() ?: return
        val playlistId = current.playlistId()
        val position = player.currentPosition.coerceAtLeast(0L)
        val key = Triple(playlistId, mediaStoreId, position)
        if (lastBookmarkWritten == key) return
        lastBookmarkWritten = key
        bookmarkStore.save(playlistId, mediaStoreId, position)
    }

    private fun snapshotState(): PlaybackState {
        val current = player.currentMediaItem
        val itemCount = player.mediaItemCount
        val queue = ArrayList<Long>(itemCount)
        val queueEntries = ArrayList<dev.maxxximgb.genesis.domain.model.QueueEntry>(itemCount)
        for (idx in 0 until itemCount) {
            val item = player.getMediaItemAt(idx)
            val msid = item.mediaStoreId() ?: 0L
            queue.add(msid)
            queueEntries.add(
                dev.maxxximgb.genesis.domain.model.QueueEntry(
                    mediaStoreId = msid,
                    sourcePlaylistId = item.sourcePlaylistId(),
                    sourceAlbumId = item.sourceAlbumId(),
                ),
            )
        }
        val currentIndex = player.currentMediaItemIndex
        return PlaybackState(
            isPlaying = player.isPlaying,
            title = current?.mediaMetadata?.title?.toString(),
            artist = current?.mediaMetadata?.artist?.toString(),
            currentMediaStoreId = current?.mediaStoreId(),
            playlistId = current?.playlistId(),
            queue = queue,
            queueEntries = queueEntries,
            currentIndex = currentIndex,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.coerceAtLeast(0L),
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode.toRepeatMode(),
            playOrderIndices = computePlayOrderIndices(currentIndex, itemCount),
        )
    }

    /**
     * Walks the player's Timeline in actual playback order, starting from [currentIndex]
     * (which becomes element 0 of the result). With shuffle off the walk degenerates to
     * `[current, current+1, ...]`. With shuffle on we follow the shuffled-next pointer until
     * we hit the end or loop back. Used by the Up Next panel so the upcoming-tracks list
     * reflects what will actually play next, not the original playlist order.
     */
    private fun computePlayOrderIndices(currentIndex: Int, itemCount: Int): List<Int> {
        if (!playOrderDirty) return cachedPlayOrder
        if (itemCount <= 0 || currentIndex < 0 || currentIndex >= itemCount) {
            cachedPlayOrder = emptyList()
            playOrderDirty = false
            return cachedPlayOrder
        }
        val timeline = player.currentTimeline
        if (timeline.isEmpty || timeline.windowCount != itemCount) {
            // Fallback when the timeline isn't ready yet (e.g., during restore): natural order.
            // Don't cache — the timeline will populate shortly and re-fire onTimelineChanged.
            return (currentIndex until itemCount).toList()
        }
        val shuffleEnabled = player.shuffleModeEnabled
        val result = ArrayList<Int>(itemCount)
        result.add(currentIndex)
        var idx = currentIndex
        while (true) {
            // REPEAT_OFF for the walk: visible queue is finite, no looping back to current.
            val next = timeline.getNextWindowIndex(idx, Player.REPEAT_MODE_OFF, shuffleEnabled)
            if (next == androidx.media3.common.C.INDEX_UNSET) break
            if (next == currentIndex || next in result) break
            result.add(next)
            idx = next
        }
        cachedPlayOrder = result
        playOrderDirty = false
        return result
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = scope.launch {
            while (isActive) {
                delay(POSITION_TICK_MS)
                val position = player.currentPosition.coerceAtLeast(0L)
                stateStore.setPosition(position)
                writeBookmark()
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    /**
     * Builds a fresh shuffle permutation that pins the current track at position 0 and
     * randomizes the rest. Required because:
     *  - `Player.setShuffleModeEnabled(true)` only flips the flag; it does NOT re-randomize.
     *  - `ShuffleOrder.DefaultShuffleOrder(N)` produces a random permutation of `[0, N)` but
     *    can land the current track anywhere — including the last position. With REPEAT_OFF
     *    that means `getNextWindowIndex` returns INDEX_UNSET immediately, the upcoming list
     *    is empty, and the Up Next panel hides itself. After a few reshuffles this also
     *    visibly desyncs the compact peek row from what's actually queued next.
     * Pinning current to shuffle position 0 guarantees `playOrderIndices` always covers all
     * other tracks, so the panel stays consistent regardless of how often the user reshuffles.
     */
    private fun regenerateShuffleOrderCurrentFirst() {
        val itemCount = player.mediaItemCount
        val current = player.currentMediaItemIndex
        if (itemCount <= 0 || current !in 0 until itemCount) return
        val others = (0 until itemCount).filter { it != current }.toMutableList()
        others.shuffle()
        val shuffled = IntArray(itemCount)
        shuffled[0] = current
        for (i in others.indices) shuffled[i + 1] = others[i]
        player.setShuffleOrder(
            ShuffleOrder.DefaultShuffleOrder(shuffled, System.nanoTime()),
        )
    }

    /**
     * Exposes our [ACTION_RESHUFFLE] custom command to controllers and handles it by
     * regenerating the player's [ShuffleOrder]. The default `Player.setShuffleModeEnabled`
     * only flips the flag — it does NOT re-randomize the order — so reshuffle goes through
     * here instead. After [ExoPlayer.setShuffleOrder] the player fires `onTimelineChanged`,
     * which is already wired to push fresh `playOrderIndices` into the state store.
     */
    private inner class ReshuffleCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands: SessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                    .buildUpon()
                    .add(SessionCommand(ACTION_RESHUFFLE, Bundle.EMPTY))
                    .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            return when (customCommand.customAction) {
                ACTION_RESHUFFLE -> {
                    if (player.shuffleModeEnabled) regenerateShuffleOrderCurrentFirst()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                else -> Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED),
                )
            }
        }
    }

    private suspend fun restoreQueueFromStore() {
        val saved = stateStore.flow.first()
        if (saved.queue.isEmpty()) return

        val tracks = libraryRepository.getTracksByIds(saved.queue)
        if (tracks.isEmpty()) return

        val items = tracks.map { it.toMediaItem(saved.playlistId) }
        val safeIndex = saved.currentIndex.coerceIn(0, items.lastIndex)

        // Mode authority for the resumed queue is [PlaylistModeStore], not [PlaybackStateStore].
        // Without this, a mode change made via the widget while the service was dead would be
        // overwritten here by the previous session's shuffle/repeat snapshot — exactly the bug
        // where "mode doesn't switch when the player is fully closed".
        val (repeat, shuffle) = saved.playlistId
            ?.let { playlistModeStore.getMode(it).toRepeatAndShuffle() }
            ?: (saved.repeatMode to saved.shuffleEnabled)

        withContext(Dispatchers.Main.immediate) {
            player.setMediaItems(items, safeIndex, saved.positionMs.coerceAtLeast(0L))
            player.shuffleModeEnabled = shuffle
            player.repeatMode = repeat.toPlayer()
            player.prepare()
            // intentionally no play() — restoration leaves the player paused
        }
    }

    private companion object {
        const val SEEK_TO_PREVIOUS_THRESHOLD_MS = 5_000L
        const val POSITION_TICK_MS = 5_000L
        const val TAG = "PlayerService"
    }
}
