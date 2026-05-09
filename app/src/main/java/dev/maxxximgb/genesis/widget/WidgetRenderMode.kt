package dev.maxxximgb.genesis.widget

import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.PlaylistDetail

enum class WidgetRenderMode {
    /** Widget has no playlist binding yet (fresh install or cleared). */
    UNBOUND,

    /** Bound playlist no longer exists. */
    MISSING_PLAYLIST,

    /** Bound playlist exists but has no tracks. */
    EMPTY_PLAYLIST,

    /**
     * This widget's playlist is the live queue and is playing. The currently-playing track is
     * present in the playlist's track list (verified by mediaStoreId match).
     */
    OWN_PLAYING,

    /** Same as OWN_PLAYING, but the player is paused. */
    OWN_PAUSED,

    /**
     * This widget's playlist is NOT the live queue (a different playlist is playing, or
     * nothing is playing) — OR it is the live queue but the current track has been removed
     * from the playlist's track list (file deleted, custom playlist edited mid-play).
     *
     * In both cases the widget renders its own state from the persisted bookmark (last-played
     * track + position for this playlist). Transport is fully active: tapping play/prev/next
     * loads this playlist on the player, replacing whatever was the active queue. This is
     * what stops a second widget from greying out the first when the user starts the second
     * — every widget keeps showing its own context.
     */
    OWN_IDLE,
}

fun widgetRenderMode(
    widgetPlaylistId: Long?,
    detail: PlaylistDetail?,
    state: PlaybackState,
): WidgetRenderMode {
    if (widgetPlaylistId == null) return WidgetRenderMode.UNBOUND
    if (detail == null) return WidgetRenderMode.MISSING_PLAYLIST
    if (detail.tracks.isEmpty()) return WidgetRenderMode.EMPTY_PLAYLIST
    val playingThis = state.playlistId == widgetPlaylistId
    if (!playingThis) return WidgetRenderMode.OWN_IDLE
    val currentInPlaylist = state.currentMediaStoreId != null &&
        detail.tracks.any { it.mediaStoreId == state.currentMediaStoreId }
    if (!currentInPlaylist) return WidgetRenderMode.OWN_IDLE
    return if (state.isPlaying) WidgetRenderMode.OWN_PLAYING else WidgetRenderMode.OWN_PAUSED
}
