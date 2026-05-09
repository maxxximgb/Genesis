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
import dev.maxxximgb.genesis.data.preferences.BookmarkStore
import dev.maxxximgb.genesis.domain.model.LoopState
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.PlaylistDetail
import dev.maxxximgb.genesis.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GenesisWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPoints.get(context.applicationContext, WidgetEntryPoint::class.java)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val audiobooksTitle = context.getString(R.string.widget_target_audiobooks)

        // Pre-fetch the persisted target + mode + background BEFORE starting Glance composition.
        // collectAsState would otherwise hand the composer its `initial` placeholder for one
        // frame (UNBOUND card, OFF loop icon, Dark theme) before the DataStore flow's first
        // emission lands — visible as a flash on cold starts and on every launcher rebind.
        // These reads are fast (DataStore in-memory cache after first hit, file I/O once).
        val initialTarget = deps.widgetPreferencesStore().getTargetFor(appWidgetId)
        val initialPid = when (initialTarget) {
            null -> null
            is WidgetTarget.Playlist -> initialTarget.playlistId
            WidgetTarget.Audiobooks -> AUDIOBOOK_PSEUDO_PLAYLIST_ID
        }
        val initialMode = initialPid?.let { deps.playlistModeStore().getMode(it) } ?: LoopState.OFF
        val initialBookmark = initialPid?.let { deps.bookmarkStore().get(it) }
        val initialBackground = deps.widgetPreferencesStore().getStoredBackgroundFor(appWidgetId)
            ?: WidgetBackgroundChoice.Dark

        provideContent {
            GlanceTheme {
                val target by deps.widgetPreferencesStore()
                    .observeTargetFor(appWidgetId)
                    .collectAsState(initial = initialTarget)
                val state by deps.playbackStateStore()
                    .flow
                    .collectAsState(initial = PlaybackState())

                val pid = when (val t = target) {
                    null -> null
                    is WidgetTarget.Playlist -> t.playlistId
                    WidgetTarget.Audiobooks -> AUDIOBOOK_PSEUDO_PLAYLIST_ID
                }

                // Detail and mode flows depend on the target type. We materialise them per
                // target so the audiobook branch doesn't reach into the playlist repository.
                val detail = when (val t = target) {
                    null -> null
                    is WidgetTarget.Playlist -> {
                        val playlist by deps.playlistRepository()
                            .observePlaylist(t.playlistId)
                            .collectAsState(initial = null)
                        val tracks by deps.playlistRepository()
                            .observePlaylistTracks(t.playlistId)
                            .collectAsState(initial = emptyList())
                        playlist?.let { PlaylistDetail(playlist = it, tracks = tracks) }
                    }
                    WidgetTarget.Audiobooks -> {
                        val tracks by remember(t) { audiobookTracksFlow(deps) }
                            .collectAsState(initial = emptyList())
                        PlaylistDetail(
                            playlist = audiobookPseudoPlaylist(audiobooksTitle),
                            tracks = tracks,
                        )
                    }
                }

                val mode = if (pid != null) {
                    val m by deps.playlistModeStore().observeMode(pid)
                        .collectAsState(initial = if (pid == initialPid) initialMode else LoopState.OFF)
                    m
                } else LoopState.OFF

                // Bookmark drives OWN_IDLE rendering (when this widget's playlist isn't on the
                // player). Without it, switching to a second widget would blank the first; with
                // it, each widget shows its own playlist's last-played track + a transport that
                // resumes from there. Observed so it stays fresh while the user listens to this
                // playlist (every position tick writes a new bookmark).
                val bookmark = if (pid != null) {
                    val b by deps.bookmarkStore().observe(pid)
                        .collectAsState(initial = if (pid == initialPid) initialBookmark else null)
                    b
                } else null

                val backgroundChoice by deps.widgetPreferencesStore()
                    .observeBackgroundFor(appWidgetId)
                    .collectAsState(initial = initialBackground)

                WidgetContent(
                    appWidgetId = appWidgetId,
                    widgetPlaylistId = pid,
                    detail = detail,
                    state = state,
                    mode = mode,
                    bookmark = bookmark,
                    backgroundChoice = backgroundChoice,
                    artLoader = deps.widgetArtLoader(),
                )
            }
        }
    }

    private fun audiobookTracksFlow(deps: WidgetEntryPoint): Flow<List<Track>> =
        combine(
            deps.userPreferencesStore().observeLibrarySort(),
            deps.userPreferencesStore().observeAudiobookOverrides(),
        ) { sort, overrides -> sort to overrides }
            .let { paired ->
                flow {
                    paired.collect { (sort, overrides) ->
                        emit(deps.mediaLibraryRepository().getAudiobookTracks(sort, overrides.toList()))
                    }
                }.flowOn(Dispatchers.IO)
            }
            // MediaStore queries can throw SecurityException if the user revokes READ_MEDIA_AUDIO
            // mid-flight via Settings → Permissions. Without a catch the throw propagates into
            // the Glance composable, which freezes the widget on its loading layout until reboot.
            .catch { emit(emptyList()) }

    private fun audiobookPseudoPlaylist(title: String): Playlist =
        Playlist(id = AUDIOBOOK_PSEUDO_PLAYLIST_ID, name = title, createdAt = 0L)

    companion object {
        // Vertical size is locked to 1 cell via widget_info.xml; only width varies.
        val SIZE_DEFAULT = DpSize(240.dp, 100.dp)
    }
}

@Composable
private fun WidgetContent(
    appWidgetId: Int,
    widgetPlaylistId: Long?,
    detail: PlaylistDetail?,
    state: PlaybackState,
    mode: LoopState,
    bookmark: BookmarkStore.Bookmark?,
    backgroundChoice: WidgetBackgroundChoice,
    artLoader: WidgetArtLoader,
) {
    val context = LocalContext.current
    val renderMode = widgetRenderMode(widgetPlaylistId, detail, state)
    // mode comes straight from PlaylistModeStore — the single source of truth. Don't fall back
    // to state.repeatMode/shuffleEnabled: that snapshot can lag (e.g., service was killed and
    // user changed mode via another widget) and would force every widget to render the stale
    // session value. PlayerService observes the same store, so the live player stays in sync.
    val effectiveMode = mode
    val params = actionParametersOf(APP_WIDGET_ID_KEY to appWidgetId)
    val openAppAction = actionStartActivity(
        // FLAG_ACTIVITY_NEW_TASK is required when starting an activity from a non-activity
        // context (Glance PendingIntents are dispatched from system context). Without it,
        // some launchers — most notably MIUI from the lockscreen widget — throw
        // ActivityNotFoundException / silently no-op. SINGLE_TOP|CLEAR_TOP are kept so
        // re-tapping while the app is already on top doesn't push a new instance.
        Intent(context, MainActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
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
        WidgetRenderMode.OWN_IDLE -> {
            // This widget's playlist isn't on the player (or the current track was removed
            // from it). Render the bookmarked track — last position the user listened from
            // *this* playlist — with full transport. Tapping play/prev/next loads this
            // playlist on the player, replacing whatever queue was active. That's the whole
            // point: a second widget starting playback no longer blanks out the first.
            val displayIndex = detail!!.tracks
                .indexOfFirst { it.mediaStoreId == bookmark?.mediaStoreId }
                .let { if (it >= 0) it else 0 }
            val displayTrack = detail.tracks[displayIndex]
            val transport = transportEnabled(
                mode = effectiveMode,
                currentIndex = displayIndex,
                lastIndex = detail.tracks.lastIndex,
            )
            val art = rememberAlbumArt(displayTrack.albumId, artLoader)
            BoundCard(
                choice = backgroundChoice,
                art = art,
                title = displayTrack.title,
                subtitle = detail.playlist.name,
                isPlaying = false,
                mode = effectiveMode,
                transport = transport,
                params = params,
                openAppAction = openAppAction,
            )
        }
        WidgetRenderMode.OWN_PLAYING, WidgetRenderMode.OWN_PAUSED -> {
            // currentIndex is guaranteed >= 0 here: widgetRenderMode returns OWN_IDLE if the
            // current track isn't in detail.tracks, so we never reach this branch with a missing track.
            val currentIndex = detail!!.tracks
                .indexOfFirst { it.mediaStoreId == state.currentMediaStoreId }
            val currentTrack = detail.tracks[currentIndex]
            val transport = transportEnabled(
                mode = effectiveMode,
                currentIndex = currentIndex,
                lastIndex = detail.tracks.lastIndex,
            )
            val art = rememberAlbumArt(currentTrack.albumId, artLoader)
            BoundCard(
                choice = backgroundChoice,
                art = art,
                title = currentTrack.title,
                subtitle = detail.playlist.name,
                isPlaying = state.isPlaying,
                mode = effectiveMode,
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

private fun resolveAccentColors(choice: WidgetBackgroundChoice): AccentColors {
    val colors = WidgetBackgroundChoice.colorsFor(choice)
    return AccentColors(
        background = ColorProvider(Color(colors.background)),
        onBackground = ColorProvider(Color(colors.onBackground)),
        onBackgroundMuted = ColorProvider(Color(colors.onBackgroundMuted)),
    )
}

@Composable
private fun BoundCard(
    choice: WidgetBackgroundChoice,
    art: Bitmap?,
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    mode: LoopState,
    transport: TransportEnabled,
    params: ActionParameters,
    openAppAction: Action,
) {
    val colors = resolveAccentColors(choice)
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
        // prev/next stay visible but turn muted (mutedColor via tintInactive) when transport
        // says they're useless — at queue edges in OFF mode, etc. Action is null in those
        // cases so the IconBtn renders without a clickable.
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
            tintActive = onColor,
            tintInactive = onColor,
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
