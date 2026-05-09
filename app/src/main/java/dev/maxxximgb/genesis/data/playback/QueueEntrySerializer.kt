package dev.maxxximgb.genesis.data.playback

import dev.maxxximgb.genesis.domain.model.QueueEntry

/**
 * Serializes [QueueEntry] tuples for DataStore. Each entry becomes
 * `mediaStoreId:sourcePlaylistId:sourceAlbumId`, with empty slots for null sources;
 * tuples are joined by `,`. Example: `42::,17:5:,99::3` is three entries — first
 * orphan, second from playlist 5, third from album 3.
 */
object QueueEntrySerializer {

    fun serialize(entries: List<QueueEntry>): String =
        entries.joinToString(",") { e ->
            buildString {
                append(e.mediaStoreId)
                append(':')
                append(e.sourcePlaylistId?.toString().orEmpty())
                append(':')
                append(e.sourceAlbumId?.toString().orEmpty())
            }
        }

    fun deserialize(csv: String): List<QueueEntry> {
        if (csv.isBlank()) return emptyList()
        return csv.split(",").mapNotNull { token ->
            val parts = token.split(':')
            if (parts.isEmpty()) return@mapNotNull null
            val msid = parts[0].toLongOrNull() ?: return@mapNotNull null
            val playlistId = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }?.toLongOrNull()
            val albumId = parts.getOrNull(2)?.takeIf { it.isNotEmpty() }?.toLongOrNull()
            QueueEntry(
                mediaStoreId = msid,
                sourcePlaylistId = playlistId,
                sourceAlbumId = albumId,
            )
        }
    }
}
