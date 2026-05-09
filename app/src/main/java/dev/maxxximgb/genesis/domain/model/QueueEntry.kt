package dev.maxxximgb.genesis.domain.model

/**
 * One slot in the playback queue, carrying the source it was enqueued from. Drives the
 * "group by source" UI in the now-playing queue panel — consecutive entries with the
 * same `sourcePlaylistId` (or `sourceAlbumId`) collapse under a single header. Entries
 * with both source ids null are "orphans" (added one-by-one without a source context)
 * and the queue UI hides them per user request.
 */
data class QueueEntry(
    val mediaStoreId: Long,
    val sourcePlaylistId: Long?,
    val sourceAlbumId: Long?,
)
