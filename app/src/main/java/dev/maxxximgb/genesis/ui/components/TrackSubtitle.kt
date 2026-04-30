package dev.maxxximgb.genesis.ui.components

import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.ui.util.formatDuration

private const val SEPARATOR = " · "
private const val EM_DASH = "—"

/**
 * Builds the iTunes-style info-rich subtitle for a track row.
 * Joins non-empty parts with " · ". Falls back to `"—"` if everything is empty.
 *
 * Examples:
 * - `"Король и Шут · Будь как сома · 3:42"`
 * - `"Король и Шут · 3:42"` (no album)
 * - `"3:42"` (no artist nor album)
 * - `"—"` (no artist, no album, zero duration)
 */
fun trackSubtitle(track: Track): String = trackSubtitle(
    artist = track.artist,
    album = track.album,
    durationMs = track.durationMs,
)

fun trackSubtitle(artist: String?, album: String?, durationMs: Long): String {
    val parts = mutableListOf<String>()
    if (!artist.isNullOrBlank()) parts += artist.trim()
    if (!album.isNullOrBlank()) parts += album.trim()
    if (durationMs > 0L) parts += formatDuration(durationMs)
    return if (parts.isEmpty()) EM_DASH else parts.joinToString(SEPARATOR)
}
