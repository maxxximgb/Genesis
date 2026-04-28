package com.maximg.player.media

import android.net.Uri

data class MediaTrack(
    val mediaStoreId: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val contentUri: Uri
)
