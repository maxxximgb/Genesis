package dev.maxxximgb.genesis.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreSource @Inject constructor(
    private val contentResolver: ContentResolver,
) {

    fun queryTracks(
        sort: SortOrder,
        query: String,
        limit: Int,
        offset: Int,
    ): List<Track> {
        val args = buildQueryArgs(sort, query, limit, offset)
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            PROJECTION,
            args,
            null,
        ) ?: return emptyList()

        return cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            val out = ArrayList<Track>(c.count)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                out.add(
                    Track(
                        mediaStoreId = id,
                        title = c.getString(titleCol).orEmpty(),
                        artist = c.getString(artistCol),
                        album = c.getString(albumCol),
                        albumId = if (c.isNull(albumIdCol)) null else c.getLong(albumIdCol),
                        durationMs = c.getLong(durationCol),
                        contentUri = contentUri.toString(),
                        dateAdded = c.getLong(dateAddedCol),
                    )
                )
            }
            out
        }
    }

    suspend fun findById(mediaStoreId: Long): Track? {
        val args = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Audio.Media._ID} = ?",
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf(mediaStoreId.toString()),
            )
        }
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            PROJECTION,
            args,
            null,
        ) ?: return null
        return cursor.use { c ->
            if (!c.moveToFirst()) return@use null
            val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
            val albumIdIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            Track(
                mediaStoreId = id,
                title = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)).orEmpty(),
                artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
                album = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)),
                albumId = if (c.isNull(albumIdIndex)) null else c.getLong(albumIdIndex),
                durationMs = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
                contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                ).toString(),
                dateAdded = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)),
            )
        }
    }

    private fun buildQueryArgs(
        sort: SortOrder,
        query: String,
        limit: Int,
        offset: Int,
    ): Bundle {
        val (selection, selectionArgs) = buildSelection(query)
        return Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sort.toMediaStoreSql())
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
        }
    }

    private fun buildSelection(query: String): Pair<String, Array<String>> {
        val base = "${MediaStore.Audio.Media.IS_MUSIC} = 1" +
            " AND ${MediaStore.Audio.Media.IS_PENDING} = 0" +
            " AND ${MediaStore.Audio.Media.IS_TRASHED} = 0"
        return if (query.isBlank()) {
            base to emptyArray()
        } else {
            val pattern = "%${query.lowercase()}%"
            val withSearch = "$base AND (LOWER(${MediaStore.Audio.Media.TITLE}) LIKE ?" +
                " OR LOWER(${MediaStore.Audio.Media.ARTIST}) LIKE ?)"
            withSearch to arrayOf(pattern, pattern)
        }
    }

    private companion object {
        val PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
        )
    }
}
