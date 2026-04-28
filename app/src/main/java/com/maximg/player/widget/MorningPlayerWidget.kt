package com.maximg.player.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.maximg.player.AppContainerProvider
import com.maximg.player.R
import com.maximg.player.playback.PlaybackController
import com.maximg.player.playback.PlaybackState

object MorningPlayerWidgetHolder {
    val widget: MorningPlayerWidget = MorningPlayerWidget()
}

class MorningPlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = AppContainerProvider.get(context)
        val playlistInfoFlow = container.widgetPreferences.observePlaylistInfo(id)
        val strings = WidgetStrings.from(context)

        provideContent {
            val playlistInfo by playlistInfoFlow.collectAsState(initial = null)
            val playbackState by container.playbackStateStore.stateFlow.collectAsState(
                initial = PlaybackState(
                    isPlaying = false,
                    title = null,
                    artist = null,
                    shuffle = false,
                    repeatMode = 0,
                    canGoPrevious = false,
                    canGoNext = false,
                    playlistId = null
                )
            )
            MorningPlayerWidgetContent(playlistInfo, playbackState, strings)
        }
    }
}

@Composable
private fun MorningPlayerWidgetContent(
    playlistInfo: WidgetPlaylistInfo?,
    playbackState: PlaybackState,
    strings: WidgetStrings
) {
    val background = ColorProvider(Color(0xFF0F172A))
    val primary = ColorProvider(Color(0xFFF97316))
    val textColor = ColorProvider(Color(0xFFFFFFFF))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .padding(12.dp)
    ) {
        val playlistName = when {
            playlistInfo?.playlistName?.isNotBlank() == true -> playlistInfo.playlistName
            playlistInfo != null -> strings.playlistTitleDefault
            else -> strings.selectPlaylist
        }
        val isCurrent = playlistInfo?.playlistId == playbackState.playlistId
        val title = if (isCurrent) {
            val track = playbackState.title ?: strings.nothingPlaying
            val artist = playbackState.artist.orEmpty()
            if (artist.isBlank()) track else "$track - $artist"
        } else {
            strings.notPlaying
        }

        val openParams = if (playlistInfo != null) {
            actionParametersOf(PlaylistIdKey to playlistInfo.playlistId)
        } else {
            actionParametersOf()
        }

        val isPlayingThisWidget = isCurrent && playbackState.isPlaying
        val canGoPreviousThisWidget = isCurrent && playbackState.canGoPrevious
        val canGoNextThisWidget = isCurrent && playbackState.canGoNext
        val loopMode = playlistInfo?.loopMode ?: PlaybackController.WidgetLoopMode.NO_REPEAT
        val prevAction = if (canGoPreviousThisWidget) actionRunCallback<WidgetPrevAction>() else null
        val nextAction = if (canGoNextThisWidget) actionRunCallback<WidgetNextAction>() else null
        val repeatAction = actionRunCallback<WidgetRepeatAction>()

        Text(
            text = playlistName,
            style = TextStyle(color = primary, fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clickable(actionRunCallback<WidgetOpenPlaylistAction>(openParams))
        )
        Text(
            text = title,
            style = TextStyle(color = textColor),
            maxLines = 1,
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        val activeTint = primary
        val inactiveTint = textColor
        val disabledTint = ColorProvider(Color(0xFF9CA3AF))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            ControlIcon(
                iconRes = R.drawable.ic_widget_prev,
                description = strings.prev,
                tint = if (canGoPreviousThisWidget) inactiveTint else disabledTint,
                onClick = prevAction
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            if (isPlayingThisWidget) {
                ControlIcon(
                    iconRes = R.drawable.ic_widget_pause,
                    description = strings.pause,
                    tint = inactiveTint,
                    onClick = actionRunCallback<WidgetPauseAction>()
                )
            } else {
                ControlIcon(
                    iconRes = R.drawable.ic_widget_play,
                    description = strings.play,
                    tint = inactiveTint,
                    onClick = actionRunCallback<WidgetPlayAction>()
                )
            }
            Spacer(modifier = GlanceModifier.width(8.dp))
            ControlIcon(
                iconRes = R.drawable.ic_widget_next,
                description = strings.next,
                tint = if (canGoNextThisWidget) inactiveTint else disabledTint,
                onClick = nextAction
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            ControlIcon(
                iconRes = loopModeIcon(loopMode),
                description = loopModeLabel(loopMode, strings),
                tint = if (loopMode == PlaybackController.WidgetLoopMode.NO_REPEAT) inactiveTint else activeTint,
                onClick = repeatAction
            )
        }
    }
}

@Composable
private fun ControlIcon(
    iconRes: Int,
    description: String,
    tint: ColorProvider,
    onClick: Action?
) {
    val modifier = if (onClick == null) {
        GlanceModifier.size(24.dp)
    } else {
        GlanceModifier
            .size(24.dp)
            .clickable(onClick)
    }
    Image(
        provider = ImageProvider(iconRes),
        contentDescription = description,
        modifier = modifier,
        colorFilter = ColorFilter.tint(tint)
    )
}

private fun loopModeLabel(mode: PlaybackController.WidgetLoopMode, strings: WidgetStrings): String {
    return when (mode) {
        PlaybackController.WidgetLoopMode.NO_REPEAT -> strings.repeat
        PlaybackController.WidgetLoopMode.REPEAT_PLAYLIST -> strings.repeatAll
        PlaybackController.WidgetLoopMode.REPEAT_TRACK -> strings.repeatOne
        PlaybackController.WidgetLoopMode.SHUFFLE -> strings.shuffle
    }
}

private fun loopModeIcon(mode: PlaybackController.WidgetLoopMode): Int {
    return when (mode) {
        PlaybackController.WidgetLoopMode.NO_REPEAT -> R.drawable.ic_widget_repeat_off
        PlaybackController.WidgetLoopMode.REPEAT_PLAYLIST -> R.drawable.ic_widget_repeat
        PlaybackController.WidgetLoopMode.REPEAT_TRACK -> R.drawable.ic_widget_repeat_one
        PlaybackController.WidgetLoopMode.SHUFFLE -> R.drawable.ic_widget_shuffle
    }
}

private data class WidgetStrings(
    val selectPlaylist: String,
    val playlistTitleDefault: String,
    val nothingPlaying: String,
    val notPlaying: String,
    val play: String,
    val pause: String,
    val prev: String,
    val next: String,
    val shuffle: String,
    val repeat: String,
    val repeatOne: String,
    val repeatAll: String
) {
    companion object {
        fun from(context: Context): WidgetStrings {
            return WidgetStrings(
                selectPlaylist = context.getString(R.string.select_playlist),
                playlistTitleDefault = context.getString(R.string.playlist_title_default),
                nothingPlaying = context.getString(R.string.nothing_playing),
                notPlaying = context.getString(R.string.not_playing),
                play = context.getString(R.string.play),
                pause = context.getString(R.string.pause),
                prev = context.getString(R.string.prev),
                next = context.getString(R.string.next),
                shuffle = context.getString(R.string.shuffle),
                repeat = context.getString(R.string.repeat),
                repeatOne = context.getString(R.string.repeat_one),
                repeatAll = context.getString(R.string.repeat_all)
            )
        }
    }
}
