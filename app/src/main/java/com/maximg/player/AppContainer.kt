package com.maximg.player

import android.content.Context
import com.maximg.player.data.AppDatabase
import com.maximg.player.data.PlaylistRepository
import com.maximg.player.media.MediaStoreRepository
import com.maximg.player.playback.PlaybackController
import com.maximg.player.playback.PlaybackStateStore
import com.maximg.player.widget.WidgetPreferences

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)

    val playlistRepository = PlaylistRepository(database.playlistDao())
    val mediaStoreRepository = MediaStoreRepository(appContext)
    val playbackStateStore = PlaybackStateStore(appContext)
    val playbackController = PlaybackController(appContext, playlistRepository, playbackStateStore)
    val widgetPreferences = WidgetPreferences(appContext)
}

object AppContainerProvider {
    @Volatile
    private var instance: AppContainer? = null

    fun get(context: Context): AppContainer {
        return instance ?: synchronized(this) {
            instance ?: AppContainer(context).also { instance = it }
        }
    }
}
