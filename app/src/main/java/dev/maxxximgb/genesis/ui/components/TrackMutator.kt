package dev.maxxximgb.genesis.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import kotlinx.coroutines.launch

/**
 * UI scope for "delete track" and "rename track" operations against MediaStore. Since
 * minSdk = 30 we use [android.provider.MediaStore.createDeleteRequest] /
 * [android.provider.MediaStore.createWriteRequest] — both return PendingIntents that
 * the system shows as a one-tap consent sheet ("Delete this audio file?", "Allow this
 * app to modify this audio file?"). The actual data-layer call lives in
 * [MediaLibraryRepository]; this composable holds the launcher + dialog state and
 * exposes ready-made [TrackAction] entries to drop into [TrackRow.actions].
 *
 * Repository is fetched via Hilt's [EntryPoint] rather than a `hiltViewModel()`
 * because this is a stateless utility — there's no per-screen state worth lifting
 * into a ViewModel, and an EntryPoint avoids forcing every host screen to wire a
 * dedicated mutator VM.
 */
class TrackMutator internal constructor(
    private val deleteFactory: (Track) -> TrackAction,
    private val renameFactory: (Track) -> TrackAction,
) {
    fun deleteAction(track: Track): TrackAction = deleteFactory.invoke(track)
    fun renameAction(track: Track): TrackAction = renameFactory.invoke(track)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TrackMutatorEntryPoint {
    fun mediaLibraryRepository(): MediaLibraryRepository
    fun playbackController(): PlaybackController
}

@Composable
fun rememberTrackMutator(
    onTrackRenamed: (Long, String) -> Unit = { _, _ -> },
    onTrackDeleted: (Long) -> Unit = {},
): TrackMutator {
    val context = LocalContext.current
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TrackMutatorEntryPoint::class.java,
        )
    }
    val repo = remember(entryPoint) { entryPoint.mediaLibraryRepository() }
    val playback = remember(entryPoint) { entryPoint.playbackController() }
    val scope = rememberCoroutineScope()
    // rememberUpdatedState so the launcher callbacks always see the latest closure
    // even though they're created once on first composition.
    val onRenamedState = androidx.compose.runtime.rememberUpdatedState(onTrackRenamed)
    val onDeletedState = androidx.compose.runtime.rememberUpdatedState(onTrackDeleted)

    var pendingRename by remember { mutableStateOf<Track?>(null) }
    // Held across the consent round-trip. When the user accepts the system's
    // delete sheet, we still need the mediaStoreId to purge our Room cache
    // (the MediaStore row + file are gone, but the join rows in
    // `playlist_tracks` and the cached row in `tracks` linger and would keep
    // showing the deleted track in any playlist that contained it).
    var pendingDeleteTrackId by remember { mutableStateOf<Long?>(null) }
    // Same idea for rename: the launcher's onResult has no parameters of its
    // own, so we stash the new title + mediaStoreId here for it to read on grant.
    var pendingRenameNewTitle by remember { mutableStateOf<String?>(null) }
    var pendingRenameTrackId by remember { mutableStateOf<Long?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val id = pendingDeleteTrackId
            if (id != null) {
                // Notify the host BEFORE the IO call so the LazyColumn can hide the
                // row immediately and animate neighbouring items into place. The
                // repository purge runs in the background and updates Room.
                onDeletedState.value(id)
                scope.launch {
                    // Pull the deleted track out of the player session so it doesn't
                    // try to play from a now-missing URI, and so it stops appearing
                    // in Up Next / Now Playing / the widget. If it's the currently
                    // playing item Media3 advances to the next track automatically.
                    runCatching { playback.removeFromQueue(id) }
                        .onFailure { android.util.Log.w("TrackMutator", "removeFromQueue threw", it) }
                    repo.purgeDeletedTrack(id)
                }
            }
        }
        pendingDeleteTrackId = null
    }

    val writeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val id = pendingRenameTrackId
            val title = pendingRenameNewTitle
            if (id != null && !title.isNullOrBlank()) {
                val trimmed = title.trim()
                // Same reasoning as delete: tell the host first so the visible row
                // re-renders with the new title via session overlay, then persist.
                onRenamedState.value(id, trimmed)
                scope.launch {
                    // Patch the renamed track's metadata in the player session so
                    // Up Next / Now Playing / the widget reflect the new title
                    // immediately, without restarting playback.
                    runCatching { playback.updateQueueItemTitle(id, trimmed) }
                        .onFailure { android.util.Log.w("TrackMutator", "updateQueueItemTitle threw", it) }
                    runCatching { repo.applyRename(id, trimmed) }
                        .onFailure { android.util.Log.e("TrackMutator", "applyRename threw", it) }
                }
            }
        }
        pendingRenameTrackId = null
        pendingRenameNewTitle = null
    }

    val deleteLabel = stringResource(R.string.delete_track)
    val renameLabel = stringResource(R.string.rename_track)

    val mutator = remember(deleteLabel, renameLabel) {
        TrackMutator(
            deleteFactory = { t ->
                TrackAction(
                    icon = Icons.Filled.Delete,
                    label = deleteLabel,
                    onClick = {
                        // Skip our own "Are you sure?" dialog — the system's delete
                        // consent sheet IS the confirmation. User had two prompts
                        // back to back; now there's just the one Android requires.
                        pendingDeleteTrackId = t.mediaStoreId
                        val pi = repo.buildDeleteRequest(listOf(t.mediaStoreId))
                        deleteLauncher.launch(
                            IntentSenderRequest.Builder(pi.intentSender).build(),
                        )
                    },
                )
            },
            renameFactory = { t ->
                TrackAction(
                    icon = Icons.Filled.Edit,
                    label = renameLabel,
                    onClick = { pendingRename = t },
                )
            },
        )
    }

    pendingRename?.let { track ->
        var fieldValue by remember(track.mediaStoreId) {
            mutableStateOf(
                TextFieldValue(
                    text = track.title,
                    // Cursor at end so an immediate edit doesn't replace the whole
                    // selected title — most users want to tweak the existing name,
                    // not retype it.
                    selection = TextRange(track.title.length),
                ),
            )
        }
        AlertDialog(
            onDismissRequest = { pendingRename = null },
            title = { Text(stringResource(R.string.rename_track_title)) },
            text = {
                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = { fieldValue = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    label = { Text(stringResource(R.string.rename_track_label)) },
                )
            },
            confirmButton = {
                val newTitle = fieldValue.text.trim()
                val canSubmit = newTitle.isNotEmpty() && newTitle != track.title
                TextButton(
                    enabled = canSubmit,
                    onClick = {
                        pendingRenameTrackId = track.mediaStoreId
                        pendingRenameNewTitle = newTitle
                        val pi = repo.buildWriteRequest(track.mediaStoreId)
                        writeLauncher.launch(
                            IntentSenderRequest.Builder(pi.intentSender).build(),
                        )
                        pendingRename = null
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRename = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    return mutator
}
