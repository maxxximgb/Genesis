package com.maximg.player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.maximg.player.MainActivity
import com.maximg.player.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayerService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var playbackStateStore: PlaybackStateStore
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        playbackStateStore = PlaybackStateStore(this)
        player = ExoPlayer.Builder(this).build()
        val sessionIntent = Intent(this, MainActivity::class.java)
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            sessionIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))

        player.addListener(object : Player.Listener {
            private fun updateNavigation() {
                val canGoPrevious = player.hasPreviousMediaItem()
                val canGoNext = player.hasNextMediaItem()
                serviceScope.launch {
                    playbackStateStore.setNavigation(
                        canGoPrevious = canGoPrevious,
                        canGoNext = canGoNext
                    )
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                serviceScope.launch {
                    playbackStateStore.setPlaying(isPlaying)
                    WidgetUpdater.updateAll(this@PlayerService)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                serviceScope.launch {
                    val title = mediaItem?.mediaMetadata?.title?.toString()
                    val artist = mediaItem?.mediaMetadata?.artist?.toString()
                    playbackStateStore.setMetadata(title, artist)
                    WidgetUpdater.updateAll(this@PlayerService)
                }
                updateNavigation()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                serviceScope.launch {
                    playbackStateStore.setShuffle(shuffleModeEnabled)
                    WidgetUpdater.updateAll(this@PlayerService)
                }
                updateNavigation()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                serviceScope.launch {
                    playbackStateStore.setRepeat(repeatMode)
                    WidgetUpdater.updateAll(this@PlayerService)
                }
                updateNavigation()
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                updateNavigation()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                updateNavigation()
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}
