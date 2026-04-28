package dev.maxxximgb.genesis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GenesisTypography,
        content = content,
    )
}
