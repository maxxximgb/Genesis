package dev.maxxximgb.genesis.domain.model

import android.provider.MediaStore

enum class SortOrder {
    DATE_ADDED_DESC,
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
    ALBUM_ASC,
    DURATION_ASC,
    DURATION_DESC;

    fun toMediaStoreSql(): String = when (this) {
        DATE_ADDED_DESC -> "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        TITLE_ASC -> "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        TITLE_DESC -> "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE DESC"
        ARTIST_ASC -> "${MediaStore.Audio.Media.ARTIST} COLLATE NOCASE ASC"
        ALBUM_ASC -> "${MediaStore.Audio.Media.ALBUM} COLLATE NOCASE ASC"
        DURATION_ASC -> "${MediaStore.Audio.Media.DURATION} ASC"
        DURATION_DESC -> "${MediaStore.Audio.Media.DURATION} DESC"
    }
}
