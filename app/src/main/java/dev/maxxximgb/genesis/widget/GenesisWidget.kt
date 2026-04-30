package dev.maxxximgb.genesis.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoints
import dev.maxxximgb.genesis.MainActivity
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.domain.model.LoopState
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.PlaylistDetail
import dev.maxxximgb.genesis.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class GenesisWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPoints.get(context.applicationContext, WidgetEntryPoint::class.java)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        // Sources are observed inside provideContent so Glance recomposition picks up
        // updates without re-running provideGlance.
        provideContent {
            GlanceTheme {
                val widgetPlaylistId by deps.widgetPreferencesStore()
                    .observePlaylistFor(appWidgetId)
                    .collectAsState(initial = null)
                val state by deps.playbackStateStore()
                    .flow
                    .collectAsState(initial = PlaybackState())
                val pid = widgetPlaylistId
                val playlist = observePerPlaylist(pid, default = null) {
                    deps.playlistRepository().observePlaylist(it)
                }
                val tracks = observePerPlaylist(pid, default = emptyList<Track>()) {
                    deps.playlistRepository().observePlaylistTracks(it)
                }
                val mode = observePerPlaylist(pid, default = LoopState.OFF) {
                    deps.playlistModeStore().observeMode(it)
                }
                val backgroundChoice by deps.widgetPreferencesStore()
                    .observeBackgroundFor(appWidgetId)
                    .collectAsState(initial = WidgetBackgroundChoice.Dynamic)

                val detail = playlist?.let { PlaylistDetail(playlist = it, tracks = tracks) }

                WidgetContent(
                    appWidgetId = appWidgetId,
                    widgetPlaylistId = pid,
                    detail = detail,
                    state = state,
                    mode = mode,
                    backgroundChoice = backgroundChoice,
                    artLoader = deps.widgetArtLoader(),
                )
            }
        }
    }

    companion object {
        // Vertical size is locked to 1 cell via widget_info.xml; only width varies.
        val SIZE_DEFAULT = DpSize(240.dp, 100.dp)
    }
}

@Composable
private fun <T> observePerPlaylist(
    playlistId: Long?,
    default: T,
    source: (Long) -> Flow<T>,
): T {
    val flow = remember(playlistId) {
        if (playlistId == null) flowOf(default) else source(playlistId)
    }
    val value by flow.collectAsState(initial = default)
    return value
}

@Composable
private fun WidgetContent(
    appWidgetId: Int,
    widgetPlaylistId: Long?,
    detail: PlaylistDetail?,
    state: PlaybackState,
    mode: LoopState,
    backgroundChoice: WidgetBackgroundChoice,
    artLoader: WidgetArtLoader,
) {
    val context = LocalContext.current
    val renderMode = widgetRenderMode(widgetPlaylistId, detail, state)
    val params = actionParametersOf(APP_WIDGET_ID_KEY to appWidgetId)
    val openAppAction = actionStartActivity(
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .apply {
                if (widgetPlaylistId != null) {
                    putExtra(MainActivity.EXTRA_PLAYLIST_ID, widgetPlaylistId)
                }
            }
    )
    val configureAction = actionStartActivity(configureWidgetIntent(context, appWidgetId))

    when (renderMode) {
        WidgetRenderMode.UNBOUND -> ConfigurePromptCard(
            titleRes = R.string.widget_unbound_title,
            subtitleRes = R.string.widget_unbound_subtitle,
            tapAction = configureAction,
        )
        WidgetRenderMode.MISSING_PLAYLIST -> ConfigurePromptCard(
            titleRes = R.string.widget_missing_playlist_title,
            subtitleRes = R.string.widget_missing_playlist_subtitle,
            tapAction = configureAction,
        )
        WidgetRenderMode.EMPTY_PLAYLIST -> EmptyPlaylistCard(
            detail = detail!!,
            tapAction = openAppAction,
        )
        WidgetRenderMode.FOREIGN -> {
            val firstTrack = detail!!.tracks.first()
            val art = rememberAlbumArt(firstTrack.albumId, artLoader)
            val accent = rememberAccent(art, backgroundChoice)
            BoundCard(
                accent = accent,
                art = art,
                title = detail.playlist.name,
                subtitle = context.getString(R.string.widget_foreign_subtitle),
                isPlaying = false,
                mode = mode,
                transport = TransportEnabled.DISABLED,
                params = params,
                openAppAction = openAppAction,
            )
        }
        WidgetRenderMode.OWN_PLAYING, WidgetRenderMode.OWN_PAUSED -> {
            val currentIndex = detail!!.tracks
                .indexOfFirst { it.mediaStoreId == state.currentMediaStoreId }
                .let { if (it < 0) 0 else it }
            val currentTrack = detail.tracks[currentIndex]
            val transport = transportEnabled(
                mode = mode,
                currentIndex = currentIndex,
                lastIndex = detail.tracks.lastIndex,
            )
            val art = rememberAlbumArt(currentTrack.albumId, artLoader)
            val accent = rememberAccent(art, backgroundChoice)
            BoundCard(
                accent = accent,
                art = art,
                title = currentTrack.title,
                subtitle = detail.playlist.name,
                isPlaying = state.isPlaying,
                mode = mode,
                transport = transport,
                params = params,
                openAppAction = openAppAction,
            )
        }
    }
}

private data class AccentColors(
    val background: ColorProvider,
    val onBackground: ColorProvider,
    val onBackgroundMuted: ColorProvider,
)

@Composable
private fun resolveAccentColors(accent: WidgetAccent?): AccentColors =
    if (accent != null) {
        AccentColors(
            background = ColorProvider(Color(accent.background)),
            onBackground = ColorProvider(Color(accent.onBackground)),
            onBackgroundMuted = ColorProvider(Color(accent.onBackgroundMuted)),
        )
    } else {
        AccentColors(
            background = GlanceTheme.colors.widgetBackground,
            onBackground = GlanceTheme.colors.onSurface,
            onBackgroundMuted = GlanceTheme.colors.onSurfaceVariant,
        )
    }

@Composable
private fun BoundCard(
    accent: WidgetAccent?,
    art: Bitmap?,
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    mode: LoopState,
    transport: TransportEnabled,
    params: ActionParameters,
    openAppAction: Action,
) {
    val colors = resolveAccentColors(accent)
    val width = LocalSize.current.width
    val buttonSize = pickButtonSize(width)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.background)
            .cornerRadius(20.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtPanel(art = art, mutedTint = colors.onBackgroundMuted)
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(openAppAction),
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = colors.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            color = colors.onBackgroundMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        maxLines = 1,
                    )
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                BoundButtonRow(
                    isPlaying = isPlaying,
                    mode = mode,
                    transport = transport,
                    params = params,
                    buttonSizeDp = buttonSize,
                    onColor = colors.onBackground,
                    mutedColor = colors.onBackgroundMuted,
                )
            }
        }
    }
}

@Composable
private fun ArtPanel(art: Bitmap?, mutedTint: ColorProvider) {
    Box(
        modifier = GlanceModifier
            .fillMaxHeight()
            .width(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) {
            Image(
                provider = ImageProvider(art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize(),
            )
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_music_note),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(mutedTint),
                modifier = GlanceModifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun ConfigurePromptCard(
    @StringRes titleRes: Int,
    @StringRes subtitleRes: Int,
    tapAction: Action,
) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .clickable(tapAction),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = context.getString(titleRes),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = context.getString(subtitleRes),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EmptyPlaylistCard(detail: PlaylistDetail, tapAction: Action) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .clickable(tapAction),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = detail.playlist.name,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = context.getString(R.string.widget_empty_playlist_subtitle),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                ),
            )
        }
    }
}

@Composable
private fun BoundButtonRow(
    isPlaying: Boolean,
    mode: LoopState,
    transport: TransportEnabled,
    params: ActionParameters,
    buttonSizeDp: Int,
    onColor: ColorProvider,
    mutedColor: ColorProvider,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBtn(
            iconRes = R.drawable.ic_widget_prev,
            cdRes = R.string.previous,
            action = if (transport.previous) actionRunCallback<PreviousTrackAction>(params) else null,
            sizeDp = buttonSizeDp,
            tintActive = onColor,
            tintInactive = mutedColor,
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        IconBtn(
            iconRes = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            cdRes = if (isPlaying) R.string.pause else R.string.play,
            action = actionRunCallback<TogglePlayPauseAction>(params),
            sizeDp = buttonSizeDp,
            tintActive = onColor,
            tintInactive = onColor,
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        IconBtn(
            iconRes = R.drawable.ic_widget_next,
            cdRes = R.string.next,
            action = if (transport.next) actionRunCallback<NextTrackAction>(params) else null,
            sizeDp = buttonSizeDp,
            tintActive = onColor,
            tintInactive = mutedColor,
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        IconBtn(
            iconRes = loopIconRes(mode),
            cdRes = loopStateContentDescription(mode),
            action = actionRunCallback<CycleLoopAction>(params),
            sizeDp = buttonSizeDp,
            tintActive = if (mode == LoopState.OFF) mutedColor else onColor,
            tintInactive = mutedColor,
        )
    }
}

@Composable
private fun IconBtn(
    iconRes: Int,
    @StringRes cdRes: Int,
    action: Action?,
    sizeDp: Int,
    tintActive: ColorProvider,
    tintInactive: ColorProvider,
) {
    val context = LocalContext.current
    val effectiveTint = if (action != null) tintActive else tintInactive
    val baseModifier = GlanceModifier.size(sizeDp.dp)
    val tappable = if (action != null) baseModifier.clickable(action) else baseModifier
    Box(
        modifier = tappable,
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = context.getString(cdRes),
            colorFilter = ColorFilter.tint(effectiveTint),
            modifier = GlanceModifier.size((sizeDp - 10).dp),
        )
    }
}

@Composable
private fun rememberAlbumArt(albumId: Long?, loader: WidgetArtLoader): Bitmap? {
    val art by produceState<Bitmap?>(initialValue = null, key1 = albumId) {
        value = loader.loadArt(albumId)
    }
    return art
}

@Composable
private fun rememberAccent(art: Bitmap?, choice: WidgetBackgroundChoice): WidgetAccent? =
    remember(art, choice) {
        when (choice) {
            WidgetBackgroundChoice.Theme -> null
            is WidgetBackgroundChoice.Solid -> WidgetAccentExtractor.fromSolid(choice.argb)
            WidgetBackgroundChoice.Dynamic ->
                runCatching { WidgetAccentExtractor.extract(art) }.getOrNull()
        }
    }

private fun pickButtonSize(width: Dp): Int = when {
    width >= 340.dp -> 40
    width >= 290.dp -> 34
    else -> 28
}

internal fun loopIconRes(mode: LoopState): Int = when (mode) {
    LoopState.OFF -> R.drawable.ic_widget_repeat_off
    LoopState.REPEAT_ALL -> R.drawable.ic_widget_repeat
    LoopState.REPEAT_ONE -> R.drawable.ic_widget_repeat_one
    LoopState.SHUFFLE -> R.drawable.ic_widget_shuffle
}

private fun configureWidgetIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, WidgetConfigActivity::class.java)
        .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
