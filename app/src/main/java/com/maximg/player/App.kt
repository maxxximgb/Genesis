package com.maximg.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.maximg.player.ui.MorningPlayerTheme
import com.maximg.player.ui.AppNavHost
import com.maximg.player.util.FONT_SCALE_KEY
import com.maximg.player.util.uiDataStore
import kotlinx.coroutines.flow.map

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

@Composable
fun MorningPlayerApp(container: AppContainer, startPlaylistId: Long?) {
    val context = LocalContext.current
    val fontScale by context.uiDataStore.data
        .map { it[FONT_SCALE_KEY] ?: 1.0f }
        .collectAsState(initial = 1.0f)

    MorningPlayerTheme {
        val baseDensity = LocalDensity.current
        CompositionLocalProvider(
            LocalAppContainer provides container,
            LocalDensity provides Density(density = baseDensity.density, fontScale = fontScale)
        ) {
            AppNavHost(startPlaylistId = startPlaylistId)
        }
    }
}
