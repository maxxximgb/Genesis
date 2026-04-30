package dev.maxxximgb.genesis.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import dev.maxxximgb.genesis.domain.model.Album
import dev.maxxximgb.genesis.domain.model.Artist
import dev.maxxximgb.genesis.domain.model.Folder
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
        filter: LibraryFilter = LibraryFilter.None,
    ): List<Track> {
        val args = buildQueryArgs(sort, query, limit, offset, filter)
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

    fun queryAlbums(): List<Album> {
        val cursor = contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            ALBUM_PROJECTION,
            null,
            null,
            "${MediaStore.Audio.Albums.ALBUM} COLLATE NOCASE ASC",
        ) ?: return emptyList()

        return cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val countCol = c.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            val out = ArrayList<Album>(c.count)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                out.add(
                    Album(
                        id = id,
                        name = c.getString(nameCol).orEmpty(),
                        artist = c.getString(artistCol),
                        trackCount = c.getInt(countCol),
                        albumArtUri = "content://media/external/audio/albumart/$id",
                    )
                )
            }
            out
        }
    }

    fun queryArtists(): List<Artist> {
        val cursor = contentResolver.query(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            ARTIST_PROJECTION,
            null,
            null,
            "${MediaStore.Audio.Artists.ARTIST} COLLATE NOCASE ASC",
        ) ?: return emptyList()

        return cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
            val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
            val out = ArrayList<Artist>(c.count)
            while (c.moveToNext()) {
                out.add(
                    Artist(
                        id = c.getLong(idCol),
                        name = c.getString(nameCol).orEmpty(),
                        trackCount = c.getInt(trackCol),
                        albumCount = c.getInt(albumCol),
                    )
                )
            }
            out
        }
    }

    /**
     * Folders are derived from the BUCKET_DISPLAY_NAME / BUCKET_ID pair in MediaStore.Audio.Media.
     * We collapse all music tracks into a unique-by-bucketId map, counting tracks per bucket.
     */
    fun queryFolders(): List<Folder> {
        val args = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                IS_MUSIC_BASE,
            )
        }
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            FOLDER_PROJECTION,
            args,
            null,
        ) ?: return emptyList()

        return cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
            val counts = HashMap<Long, Pair<String, Int>>()
            while (c.moveToNext()) {
                if (c.isNull(idCol)) continue
                val id = c.getLong(idCol)
                val name = c.getString(nameCol).orEmpty()
                val existing = counts[id]
                counts[id] = if (existing == null) name to 1 else existing.first to existing.second + 1
            }
            counts.entries
                .map { (id, pair) -> Folder(bucketId = id, displayName = pair.first, trackCount = pair.second) }
                .sortedBy { it.displayName.lowercase() }
        }
    }

    private fun buildQueryArgs(
        sort: SortOrder,
        query: String,
        limit: Int,
        offset: Int,
        filter: LibraryFilter,
    ): Bundle {
        val (selection, selectionArgs) = buildSelection(query, filter)
        return Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sort.toMediaStoreSql())
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
        }
    }

    private fun buildSelection(query: String, filter: LibraryFilter): Pair<String, Array<String>> {
        val clauses = mutableListOf(IS_MUSIC_BASE)
        val args = mutableListOf<String>()
        when (filter) {
            LibraryFilter.None -> Unit
            is LibraryFilter.ByAlbum -> {
                clauses += "${MediaStore.Audio.Media.ALBUM_ID} = ?"
                args += filter.albumId.toString()
            }
            is LibraryFilter.ByArtist -> {
                clauses += "${MediaStore.Audio.Media.ARTIST_ID} = ?"
                args += filter.artistId.toString()
            }
            is LibraryFilter.ByFolder -> {
                clauses += "${MediaStore.Audio.Media.BUCKET_ID} = ?"
                args += filter.bucketId.toString()
            }
        }
        if (query.isNotBlank()) {
            val pattern = "%${query.lowercase()}%"
            clauses += "(LOWER(${MediaStore.Audio.Media.TITLE}) LIKE ?" +
                " OR LOWER(${MediaStore.Audio.Media.ARTIST}) LIKE ?)"
            args += pattern
            args += pattern
        }
        return clauses.joinToString(" AND ") to args.toTypedArray()
    }

    private companion object {
        const val IS_MUSIC_BASE = "${MediaStore.Audio.Media.IS_MUSIC} = 1" +
            " AND ${MediaStore.Audio.Media.IS_PENDING} = 0" +
            " AND ${MediaStore.Audio.Media.IS_TRASHED} = 0"

        val PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
        )

        val ALBUM_PROJECTION = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
        )

        val ARTIST_PROJECTION = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
        )

        val FOLDER_PROJECTION = arrayOf(
            MediaStore.Audio.Media.BUCKET_ID,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
        )
    }
}
