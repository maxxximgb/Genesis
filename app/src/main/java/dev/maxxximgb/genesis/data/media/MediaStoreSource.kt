package dev.maxxximgb.genesis.data.media

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import dev.maxxximgb.genesis.data.local.dao.TrackTitleOverrideDao
import dev.maxxximgb.genesis.data.local.entity.TrackTitleOverrideEntity
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
    private val titleOverrideDao: TrackTitleOverrideDao,
) {

    /**
     * Build a delete-consent PendingIntent for [mediaStoreIds]. The system shows a
     * one-tap "Delete?" sheet — on grant, the system itself removes the rows from
     * MediaStore AND the underlying files; the app doesn't need to do a follow-up
     * write. minSdk = 30 (Android 11) so we use the modern API directly without the
     * RecoverableSecurityException retry dance.
     */
    fun buildDeleteRequest(mediaStoreIds: List<Long>): PendingIntent {
        val uris = mediaStoreIds.map { audioUri(it) }
        return MediaStore.createDeleteRequest(contentResolver, uris)
    }

    /**
     * Build a write-consent PendingIntent for [mediaStoreId]. Required before we can
     * `update()` any field on a track the app doesn't own (i.e., everything in the
     * shared MediaStore). After the user grants consent, call [applyRename] to do the
     * actual TITLE / DISPLAY_NAME write.
     */
    fun buildWriteRequest(mediaStoreId: Long): PendingIntent {
        return MediaStore.createWriteRequest(contentResolver, listOf(audioUri(mediaStoreId)))
    }

    /**
     * Rename a track for [mediaStoreId]. The user-visible title is stored as a local
     * override (see [applyTitleOverrides]); we do NOT round-trip TITLE through
     * MediaStore because that's broken in opposite ways on different vendors:
     * - On most devices, TITLE survives the SQL update but the very next file rescan
     *   (triggered by the DISPLAY_NAME write below) re-derives it from the file's
     *   embedded metadata or filename, clobbering our value.
     * - On some Samsung One UI builds, MediaProvider rejects TITLE-only updates
     *   outright (`update()` returns 0) even with a granted write request.
     * As a courtesy we still rename the file on disk via DISPLAY_NAME so the new name
     * shows up in other apps (file managers, MTP); the original extension is preserved
     * and FAT/exFAT-unsafe characters are sanitised. Caller must have already received
     * a granted result from [buildWriteRequest]'s PendingIntent — otherwise the
     * DISPLAY_NAME write throws SecurityException (which we catch as non-fatal).
     */
    fun applyRename(mediaStoreId: Long, newTitle: String): Boolean {
        val current = currentDisplayName(mediaStoreId)
        val extension = current?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() }
        val safeBase = sanitiseFilenameBase(newTitle)
        val newDisplayName = if (extension == null) safeBase else "$safeBase.$extension"
        val uri = audioUri(mediaStoreId)

        // Persist the user's chosen title locally. This is the source of truth for
        // the visible title from now on — Android 11+ MediaStore will not let us
        // round-trip TITLE reliably (rename triggers a rescan that re-derives
        // TITLE; some OEMs reject TITLE-only updates outright), so we overlay this
        // value on every read instead of fighting the platform.
        try {
            titleOverrideDao.upsert(TrackTitleOverrideEntity(mediaStoreId, newTitle))
        } catch (t: Throwable) {
            Log.e(TAG, "applyRename failed to upsert override for id=$mediaStoreId", t)
            return false
        }

        // Best-effort rename on disk too, so the file shows up with the user's name in
        // other apps (file managers, MTP). DISPLAY_NAME updates are well-supported on
        // Android 11+; failures here don't affect our in-app display.
        if (current != newDisplayName) {
            try {
                val v = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, newDisplayName)
                }
                contentResolver.update(uri, v, null, null)
            } catch (t: Throwable) {
                Log.w(TAG, "applyRename DISPLAY_NAME write failed (non-fatal)", t)
            }
        }
        return true
    }

    /** Removes any local override row for [mediaStoreId]. Called from delete flow. */
    fun clearTitleOverride(mediaStoreId: Long) {
        try {
            titleOverrideDao.delete(mediaStoreId)
        } catch (t: Throwable) {
            Log.w(TAG, "clearTitleOverride failed for id=$mediaStoreId", t)
        }
    }

    /** Overlay any user-rename overrides onto a list of MediaStore-derived tracks. */
    private fun applyTitleOverrides(tracks: List<Track>): List<Track> {
        if (tracks.isEmpty()) return tracks
        val overrides = try {
            titleOverrideDao.getByIds(tracks.map { it.mediaStoreId })
                .associate { it.mediaStoreId to it.title }
        } catch (t: Throwable) {
            Log.w(TAG, "applyTitleOverrides lookup failed", t)
            return tracks
        }
        if (overrides.isEmpty()) return tracks
        return tracks.map { t ->
            overrides[t.mediaStoreId]?.let { t.copy(title = it) } ?: t
        }
    }

    private fun currentDisplayName(mediaStoreId: Long): String? {
        val cursor = contentResolver.query(
            audioUri(mediaStoreId),
            arrayOf(MediaStore.Audio.Media.DISPLAY_NAME),
            null,
            null,
            null,
        ) ?: return null
        return cursor.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }

    private fun sanitiseFilenameBase(name: String): String {
        val cleaned = name
            .replace(UNSAFE_FILENAME_CHARS, "_")
            .trim()
            .trimEnd('.')
        return cleaned.ifEmpty { "Untitled" }
    }

    private fun audioUri(mediaStoreId: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)

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
        return applyTitleOverrides(cursor.use { it.readAllTracks() })
    }

    suspend fun queryByIds(ids: List<Long>): List<Track> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val args = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Audio.Media._ID} IN ($placeholders)",
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                ids.map { it.toString() }.toTypedArray(),
            )
        }
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            PROJECTION,
            args,
            null,
        ) ?: return emptyList()
        return applyTitleOverrides(cursor.use { it.readAllTracks() })
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
        val raw = cursor.use { it.readAllTracks() }
        return applyTitleOverrides(raw).firstOrNull()
    }

    private fun Cursor.readAllTracks(): List<Track> {
        val idCol = getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumIdCol = getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val durationCol = getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val dateAddedCol = getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val out = ArrayList<Track>(count)
        while (moveToNext()) {
            val id = getLong(idCol)
            out.add(
                Track(
                    mediaStoreId = id,
                    title = getString(titleCol).orEmpty(),
                    artist = getString(artistCol),
                    album = getString(albumCol),
                    albumId = if (isNull(albumIdCol)) null else getLong(albumIdCol),
                    durationMs = getLong(durationCol),
                    contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    ).toString(),
                    dateAdded = getLong(dateAddedCol),
                ),
            )
        }
        return out
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
        // Audiobooks have IS_MUSIC=0, so we swap the base predicate when filtering for them.
        // When the user has flagged extra tracks via the kebab menu, those mediaStoreIds are
        // OR-ed into the audiobook predicate so the union appears in one paged query.
        val basePredicate = when (filter) {
            is LibraryFilter.Audiobooks -> {
                if (filter.overrideIds.isEmpty()) {
                    IS_AUDIOBOOK_BASE
                } else {
                    val placeholders = filter.overrideIds.joinToString(",") { "?" }
                    "(${MediaStore.Audio.Media.IS_AUDIOBOOK} = 1" +
                        " OR ${MediaStore.Audio.Media._ID} IN ($placeholders))" +
                        " AND ${MediaStore.Audio.Media.IS_PENDING} = 0" +
                        " AND ${MediaStore.Audio.Media.IS_TRASHED} = 0"
                }
            }
            else -> IS_MUSIC_BASE
        }
        val clauses = mutableListOf(basePredicate)
        val args = mutableListOf<String>()
        when (filter) {
            LibraryFilter.None -> Unit
            is LibraryFilter.Audiobooks -> {
                args.addAll(filter.overrideIds.map { it.toString() })
            }
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
        const val TAG = "MediaStoreSource"

        // Characters that are unsafe in FAT/exFAT filenames (relevant when the
        // track lives on an SD-card volume), plus ASCII control bytes. Matches
        // get rewritten to `_` before the on-disk DISPLAY_NAME write.
        val UNSAFE_FILENAME_CHARS = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")

        const val IS_MUSIC_BASE = "${MediaStore.Audio.Media.IS_MUSIC} = 1" +
            " AND ${MediaStore.Audio.Media.IS_PENDING} = 0" +
            " AND ${MediaStore.Audio.Media.IS_TRASHED} = 0"

        const val IS_AUDIOBOOK_BASE = "${MediaStore.Audio.Media.IS_AUDIOBOOK} = 1" +
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
