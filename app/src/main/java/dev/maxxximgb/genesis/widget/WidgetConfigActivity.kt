package dev.maxxximgb.genesis.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

    LaunchedEffect(appWidgetId) {
        viewModel.hydrateBackground(appWidgetId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.widget_choose_playlist)) })
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (playlists.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.widget_no_playlists_empty),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
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
            BgModeChip(
                label = stringResource(R.string.widget_bg_dynamic),
                selected = selected is WidgetBackgroundChoice.Dynamic,
                onClick = { onSelected(WidgetBackgroundChoice.Dynamic) },
                preview = { DynamicPreviewSwatch() },
            )
            BgModeChip(
                label = stringResource(R.string.widget_bg_theme),
                selected = selected is WidgetBackgroundChoice.Theme,
                onClick = { onSelected(WidgetBackgroundChoice.Theme) },
                preview = { ThemePreviewSwatch() },
            )
            BgModeChip(
                label = stringResource(R.string.widget_bg_solid),
                selected = selected is WidgetBackgroundChoice.Solid,
                onClick = {
                    val current = (selected as? WidgetBackgroundChoice.Solid)?.argb
                        ?: WidgetBackgroundChoice.PRESETS.first()
                    onSelected(WidgetBackgroundChoice.Solid(current))
                },
                preview = {
                    val argb = (selected as? WidgetBackgroundChoice.Solid)?.argb
                        ?: WidgetBackgroundChoice.PRESETS.first()
                    SolidPreviewSwatch(argb)
                },
            )
        }
        if (selected is WidgetBackgroundChoice.Solid) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WidgetBackgroundChoice.PRESETS.forEach { argb ->
                    SolidColorTile(
                        argb = argb,
                        selected = selected.argb == argb,
                        onClick = { onSelected(WidgetBackgroundChoice.Solid(argb)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BgModeChip(
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
private fun DynamicPreviewSwatch() {
    val colors = listOf(
        Color(0xFFE0506B),
        Color(0xFF3D6EE0),
        Color(0xFF7FAE3A),
    )
    Row(
        modifier = Modifier
            .size(width = 24.dp, height = 16.dp)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color),
            )
        }
    }
}

@Composable
private fun ThemePreviewSwatch() {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(4.dp),
            ),
    )
}

@Composable
private fun SolidPreviewSwatch(argb: Int) {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(argb)),
    )
}

@Composable
private fun SolidColorTile(argb: Int, selected: Boolean, onClick: () -> Unit) {
    val borderWidth = if (selected) 3.dp else 1.dp
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(argb))
            .border(width = borderWidth, color = borderColor, shape = CircleShape)
            .clickable(onClick = onClick),
    )
}
