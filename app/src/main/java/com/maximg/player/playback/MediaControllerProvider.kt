package com.maximg.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.util.UnstableApi
import com.maximg.player.util.await
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(UnstableApi::class)
object MediaControllerProvider {
    @Volatile
    private var controller: MediaController? = null
    @Volatile
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val mutex = Mutex()

    suspend fun get(context: Context): MediaController {
        controller?.let { return it }
        return mutex.withLock {
            controller?.let { return it }

            val appContext = context.applicationContext
            val firstAttempt = runCatching {
                getOrCreateFuture(appContext).await()
            }
            val resolvedController = if (firstAttempt.isSuccess) {
                firstAttempt.getOrThrow()
            } else {
                controllerFuture?.cancel(true)
                controllerFuture = null
                getOrCreateFuture(appContext).await()
            }
            controller = resolvedController
            resolvedController
        }
    }

    private fun getOrCreateFuture(appContext: Context): ListenableFuture<MediaController> {
        return controllerFuture ?: run {
            val token = SessionToken(appContext, ComponentName(appContext, PlayerService::class.java))
            MediaController.Builder(appContext, token).buildAsync().also { controllerFuture = it }
        }
    }

    fun release() {
        controller?.release()
        controller = null
        controllerFuture?.cancel(true)
        controllerFuture = null
    }
}
