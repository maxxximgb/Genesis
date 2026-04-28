package dev.maxxximgb.genesis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun GenesisTheme(
    viewModel: ThemeViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val themeState by viewModel.themeState.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeState.mode) {
        ThemeMode.AUTO -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = resolvePalette(themeState.palette, useDark)
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity.density, themeState.fontScale) {
        Density(density = baseDensity.density, fontScale = themeState.fontScale)
    }
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GenesisTypography,
            content = content,
        )
    }
}
