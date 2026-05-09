package dev.maxxximgb.genesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User-renamed titles, keyed by MediaStore _ID. Lives separately from [TrackEntity]
 * because that table only has rows for tracks we've cached for playlist/now-playing
 * use, whereas a user can rename ANY track they see in the library — even one not
 * yet referenced by a playlist.
 *
 * The override is preferred over [android.provider.MediaStore.Audio.Media.TITLE]
 * when one exists. We can't reliably write TITLE back to MediaStore on Android
 * 11+ (some OEMs revert it during the rename-triggered rescan, others reject the
 * write outright), so this table is the source of truth for the user-visible name.
 */
@Entity(tableName = "track_title_overrides")
data class TrackTitleOverrideEntity(
    @PrimaryKey val mediaStoreId: Long,
    val title: String,
)
