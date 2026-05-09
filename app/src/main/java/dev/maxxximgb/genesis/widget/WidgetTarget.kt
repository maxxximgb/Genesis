package dev.maxxximgb.genesis.widget

/**
 * Target a widget is bound to. Until 2.3 widgets could only point at user playlists; now an
 * audiobook target lets a widget surface the whole audiobook library (native + overrides).
 *
 * The audiobook queue uses a reserved sentinel id [AUDIOBOOK_PSEUDO_PLAYLIST_ID] inside the
 * playback layer (PlaybackState.playlistId, MediaItem extras, BookmarkStore, PlaylistModeStore)
 * so the existing per-context plumbing keeps working without an extra "scope" enum.
 */
sealed interface WidgetTarget {
    data class Playlist(val playlistId: Long) : WidgetTarget
    data object Audiobooks : WidgetTarget
}

/**
 * Sentinel playlistId used in PlaybackState / BookmarkStore / PlaylistModeStore to mean
 * "the audiobook queue, not a user playlist". Long.MIN_VALUE keeps it unambiguously outside
 * the range MediaStore ever produces for real Room playlist IDs (which start at 1).
 */
const val AUDIOBOOK_PSEUDO_PLAYLIST_ID: Long = Long.MIN_VALUE

fun Long?.isAudiobookTarget(): Boolean = this == AUDIOBOOK_PSEUDO_PLAYLIST_ID

internal const val WIDGET_TARGET_AUDIOBOOKS = "audiobooks"
internal const val WIDGET_TARGET_PLAYLIST_PREFIX = "playlist:"

internal fun WidgetTarget.encode(): String = when (this) {
    is WidgetTarget.Playlist -> "$WIDGET_TARGET_PLAYLIST_PREFIX$playlistId"
    WidgetTarget.Audiobooks -> WIDGET_TARGET_AUDIOBOOKS
}

internal fun decodeWidgetTarget(raw: String?): WidgetTarget? {
    if (raw == null) return null
    return when {
        raw == WIDGET_TARGET_AUDIOBOOKS -> WidgetTarget.Audiobooks
        raw.startsWith(WIDGET_TARGET_PLAYLIST_PREFIX) ->
            raw.removePrefix(WIDGET_TARGET_PLAYLIST_PREFIX).toLongOrNull()
                ?.let { WidgetTarget.Playlist(it) }
        else -> null
    }
}
