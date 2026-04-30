package dev.maxxximgb.genesis.service

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import dev.maxxximgb.genesis.data.playback.PlaybackStateStore
import dev.maxxximgb.genesis.data.playback.mediaStoreId
import dev.maxxximgb.genesis.data.playback.playlistId
import dev.maxxximgb.genesis.data.playback.toMediaItem
import dev.maxxximgb.genesis.data.playback.toPlayer
import dev.maxxximgb.genesis.data.playback.toRepeatMode
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionTickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()

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

        session = MediaSession.Builder(this, player).build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build()
        )

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                writeState()
                if (isPlaying) startPositionTicker() else stopPositionTicker()
            }

            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                writeState()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                writeState()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                writeState()
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                writeState()
            }
        })

        scope.launch { restoreQueueFromStore() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = session

    override fun onDestroy() {
        stopPositionTicker()
        scope.cancel()
        session.release()
        player.release()
        super.onDestroy()
    }

    private fun writeState() {
        scope.launch {
            stateStore.update(snapshotState())
        }
        widgetUpdater.requestUpdate()
    }

    private fun snapshotState(): PlaybackState {
        val current = player.currentMediaItem
        val queue = (0 until player.mediaItemCount).map { idx ->
            player.getMediaItemAt(idx).mediaStoreId() ?: 0L
        }
        return PlaybackState(
            isPlaying = player.isPlaying,
            title = current?.mediaMetadata?.title?.toString(),
            artist = current?.mediaMetadata?.artist?.toString(),
            currentMediaStoreId = current?.mediaStoreId(),
            playlistId = current?.playlistId(),
            queue = queue,
            currentIndex = player.currentMediaItemIndex,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.coerceAtLeast(0L),
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode.toRepeatMode(),
        )
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = scope.launch {
            while (isActive) {
                delay(POSITION_TICK_MS)
                stateStore.setPosition(player.currentPosition.coerceAtLeast(0L))
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    private suspend fun restoreQueueFromStore() {
        val saved = stateStore.flow.first()
        if (saved.queue.isEmpty()) return

        val tracks = saved.queue.mapNotNull { libraryRepository.findById(it) }
        if (tracks.isEmpty()) return

        val items = tracks.map { it.toMediaItem(saved.playlistId) }
        val safeIndex = saved.currentIndex.coerceIn(0, items.lastIndex)

        withContext(Dispatchers.Main.immediate) {
            player.setMediaItems(items, safeIndex, saved.positionMs.coerceAtLeast(0L))
            player.shuffleModeEnabled = saved.shuffleEnabled
            player.repeatMode = saved.repeatMode.toPlayer()
            player.prepare()
            // intentionally no play() — restoration leaves the player paused
        }
    }

    private companion object {
        const val SEEK_TO_PREVIOUS_THRESHOLD_MS = 5_000L
        const val POSITION_TICK_MS = 5_000L
    }
}
