package com.maximg.player.util

import java.util.Locale

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) {
        return "0:00"
    }

    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val locale = Locale.getDefault()

    return if (hours > 0) {
        String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(locale, "%d:%02d", minutes, seconds)
    }
}
