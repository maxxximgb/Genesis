package com.maximg.player.media

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.maximg.player.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepository(private val context: Context) {
    suspend fun queryTracks(searchQuery: String?): List<MediaTrack> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )

        val baseSelection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val (selection, args) = if (searchQuery.isNullOrBlank()) {
            baseSelection to emptyArray()
        } else {
            val like = "%${searchQuery.trim()}%"
            "$baseSelection AND (${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?)" to arrayOf(like, like)
        }

        val results = mutableListOf<MediaTrack>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: context.getString(R.string.unknown)
                val artist = cursor.getString(artistCol)
                val album = cursor.getString(albumCol)
                val duration = cursor.getLong(durationCol)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                results.add(
                    MediaTrack(
                        mediaStoreId = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        contentUri = contentUri
                    )
                )
            }
        }
        results
    }
}
