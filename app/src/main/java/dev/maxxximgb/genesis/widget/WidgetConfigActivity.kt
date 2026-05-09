package dev.maxxximgb.genesis.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.ui.theme.GenesisTheme

@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        setResult(Activity.RESULT_CANCELED, resultIntent(appWidgetId))

        setContent {
            GenesisTheme {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    onPlaylistChosen = ::commitChoice,
                )
            }
        }
    }

    private fun commitChoice(appWidgetId: Int) {
        setResult(Activity.RESULT_OK, resultIntent(appWidgetId))
        finish()
    }

    private fun resultIntent(appWidgetId: Int): Intent =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    appWidgetId: Int,
    onPlaylistChosen: (appWidgetId: Int) -> Unit,
    viewModel: WidgetConfigViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isBinding by viewModel.isBinding.collectAsStateWithLifecycle()
    val background by viewModel.selectedBackground.collectAsStateWithLifecycle()
    val audiobookCount by viewModel.audiobookCount.collectAsStateWithLifecycle()
    val isSystemInDark = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

    LaunchedEffect(appWidgetId) {
        viewModel.hydrateBackground(appWidgetId, isSystemInDark)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.widget_choose_target)) })
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Render the picker even when there are no playlists — we still want to expose
            // the "All audiobooks" target. We only show the "no content at all" empty state
            // when both lists are empty.
            val hasAnything = playlists.isNotEmpty() || audiobookCount > 0
            if (!hasAnything) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.widget_no_targets_empty),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item(key = "audiobooks-target") {
                        AudiobooksItem(
                            count = audiobookCount,
                            enabled = !isBinding && audiobookCount > 0,
                        ) {
                            viewModel.bindAudiobooks(appWidgetId) {
                                onPlaylistChosen(appWidgetId)
                            }
                        }
                        HorizontalDivider()
                    }
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistItem(playlist = playlist, enabled = !isBinding) {
                            viewModel.bind(appWidgetId, playlist.id) {
                                onPlaylistChosen(appWidgetId)
                            }
                        }
                        HorizontalDivider()
                    }
                    item(key = "background-picker") {
                        BackgroundPicker(
                            selected = background,
                            onSelected = viewModel::onBackgroundChosen,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistItem(playlist: Playlist, enabled: Boolean, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        leadingContent = {
            Icon(Icons.Filled.QueueMusic, contentDescription = null)
        },
        headlineContent = { Text(playlist.name) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudiobooksItem(count: Int, enabled: Boolean, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        leadingContent = {
            Icon(Icons.Filled.MenuBook, contentDescription = null)
        },
        headlineContent = { Text(stringResource(R.string.widget_target_audiobooks)) },
        supportingContent = {
            val label = if (count > 0) {
                stringResource(R.string.widget_audiobook_count_fmt, count)
            } else {
                stringResource(R.string.widget_audiobook_empty)
            }
            Text(label)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackgroundPicker(
    selected: WidgetBackgroundChoice,
    onSelected: (WidgetBackgroundChoice) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
        Text(
            text = stringResource(R.string.widget_bg_label),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BgChip(
                label = stringResource(R.string.widget_bg_dark),
                selected = selected == WidgetBackgroundChoice.Dark,
                onClick = { onSelected(WidgetBackgroundChoice.Dark) },
                preview = { Swatch(WidgetBackgroundChoice.Dark) },
            )
            BgChip(
                label = stringResource(R.string.widget_bg_light),
                selected = selected == WidgetBackgroundChoice.Light,
                onClick = { onSelected(WidgetBackgroundChoice.Light) },
                preview = { Swatch(WidgetBackgroundChoice.Light) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BgChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = preview,
        shape = FilterChipDefaults.shape,
    )
}

@Composable
private fun Swatch(choice: WidgetBackgroundChoice) {
    val colors = WidgetBackgroundChoice.colorsFor(choice)
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(colors.background))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(4.dp),
            ),
    )
}
