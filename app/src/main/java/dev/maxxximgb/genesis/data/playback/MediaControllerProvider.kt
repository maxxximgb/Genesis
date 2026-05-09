package dev.maxxximgb.genesis.data.playback

import android.content.ComponentName
import android.content.Context
import androidx.concurrent.futures.await
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.maxxximgb.genesis.service.PlayerService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaControllerProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val mutex = Mutex()

    @Volatile
    private var cached: MediaController? = null

    suspend fun get(): MediaController = mutex.withLock {
        cached?.takeIf { it.isConnected } ?: connect().also { cached = it }
    }

    private suspend fun connect(): MediaController {
        val token = SessionToken(context, ComponentName(context, PlayerService::class.java))
        // Listener.onDisconnected fires when the bound MediaSession dies (service stopped or
        // crashed). Without this hook, [cached] would keep returning a corpse: isConnected can
        // briefly stay true after release, and any controller call then throws IllegalStateException.
        // Volatile write is enough — we don't need to grab the mutex; concurrent get() readers
        // either see the old reference (whose isConnected returns false → reconnect) or null
        // (→ reconnect). Reconnect itself is mutex-serialised.
        val listener = object : MediaController.Listener {
            override fun onDisconnected(controller: MediaController) {
                if (cached === controller) cached = null
            }
        }
        return try {
            MediaController.Builder(context, token).setListener(listener).buildAsync().await()
        } catch (t: Throwable) {
            // one retry on transient bind failures
            MediaController.Builder(context, token).setListener(listener).buildAsync().await()
        }
    }

    fun release() {
        cached?.release()
        cached = null
    }
}
