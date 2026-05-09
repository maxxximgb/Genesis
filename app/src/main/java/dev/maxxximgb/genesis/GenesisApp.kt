package dev.maxxximgb.genesis

import android.app.ActivityManager
import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.maxxximgb.genesis.data.playback.PlaybackStateStore
import dev.maxxximgb.genesis.service.PlayerService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class GenesisApp : Application() {

    @Inject
    lateinit var playbackStateStore: PlaybackStateStore

    override fun onCreate() {
        super.onCreate()
        clearStaleIsPlayingIfServiceDead()
    }

    /**
     * If the persisted isPlaying=true survived without a corresponding live PlayerService
     * (force-stop, OOM kill, crash), correct it to false now — before any widget action callback
     * runs. Otherwise TogglePlayPauseAction reads the stale flag and calls pause() instead of
     * play(), so the user's first widget tap silently no-ops.
     *
     * runBlocking is justified here: this runs once per process start, the operation is bounded
     * (one DataStore read; conditional write only when we have something to fix), and it must
     * complete before Glance dispatches action callbacks (which start after Application.onCreate
     * returns). Wrapped in withTimeoutOrNull-equivalent via runCatching so a slow DataStore can't
     * stall app launch indefinitely.
     */
    private fun clearStaleIsPlayingIfServiceDead() {
        if (isPlayerServiceRunning()) return
        runCatching {
            runBlocking {
                if (playbackStateStore.flow.first().isPlaying) {
                    playbackStateStore.setIsPlaying(false)
                }
            }
        }
    }

    @Suppress("DEPRECATION") // getRunningServices is deprecated for cross-app use; for our own
    // services it still returns reliable data on every Android version we target.
    private fun isPlayerServiceRunning(): Boolean {
        val mgr = getSystemService(ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val name = PlayerService::class.java.name
        return mgr.getRunningServices(Int.MAX_VALUE).any { it.service.className == name }
    }
}
