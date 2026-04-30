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

    /** Some other playlist (or single track) is currently playing. */
    FOREIGN,

    /** This widget's playlist is currently playing and the player is playing. */
    OWN_PLAYING,

    /** This widget's playlist is loaded but paused. */
    OWN_PAUSED,
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
    if (!playingThis) return WidgetRenderMode.FOREIGN
    return if (state.isPlaying) WidgetRenderMode.OWN_PLAYING else WidgetRenderMode.OWN_PAUSED
}
