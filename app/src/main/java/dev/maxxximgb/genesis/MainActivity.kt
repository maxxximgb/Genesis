package dev.maxxximgb.genesis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dagger.hilt.android.AndroidEntryPoint
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.ui.theme.GenesisTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: MediaLibraryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GenesisTheme {
                // TEMP: replaced by LibraryScreen + PermissionGate in 1.5
                DebugLibraryRoute(repository)
            }
        }
    }
}

// TEMP: 1.2 debug surface — entire composable removed in 1.5.
@Composable
private fun DebugLibraryRoute(repository: MediaLibraryRepository) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> permissionGranted = granted },
    )
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!permissionGranted) launcher.launch(Manifest.permission.READ_MEDIA_AUDIO)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Genesis · iter 1.2 (debug)",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            if (!permissionGranted) {
                Text(
                    text = "READ_MEDIA_AUDIO required.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }
            val pager = remember { repository.pagedLibrary(SortOrder.DATE_ADDED_DESC, "") }
            val items = pager.flow.collectAsLazyPagingItems()
            DebugTrackList(items)
        }
    }
}

@Composable
private fun DebugTrackList(items: LazyPagingItems<Track>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = items.itemCount,
            key = items.itemKey { it.mediaStoreId },
        ) { index ->
            val track = items[index] ?: return@items
            ListItem(
                headlineContent = {
                    Text(
                        text = track.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Text(
                        text = track.artist ?: "—",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}
