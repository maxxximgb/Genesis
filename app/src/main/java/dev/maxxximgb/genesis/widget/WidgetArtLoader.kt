package dev.maxxximgb.genesis.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.maxxximgb.genesis.data.media.AlbumArt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetArtLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val loader: ImageLoader by lazy { ImageLoader.Builder(context).build() }
    private val cache = object : LinkedHashMap<Long, Bitmap>(0, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Bitmap>): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    suspend fun loadArt(albumId: Long?): Bitmap? {
        val id = albumId ?: return null
        synchronized(cache) { cache[id] }?.let { return it }

        val uri = AlbumArt.uri(id) ?: return null
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(ART_SIZE_PX)
            .allowHardware(false)
            .build()
        val bitmap = (loader.execute(request).drawable as? BitmapDrawable)?.bitmap ?: return null
        synchronized(cache) { cache[id] = bitmap }
        return bitmap
    }

    private companion object {
        const val ART_SIZE_PX = 256
        const val MAX_CACHE_ENTRIES = 32
    }
}
