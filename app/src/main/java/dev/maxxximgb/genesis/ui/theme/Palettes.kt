package dev.maxxximgb.genesis.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

enum class PaletteId {
    ITUNES;

    companion object {
        val DEFAULT = ITUNES

        fun fromName(name: String?): PaletteId =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

// Light mode uses the iTunes/macOS system blue; dark mode is fully grayscale — selection
// is shown via brightness contrast, not a colored accent.
private val LightAccent = Color(0xFF007AFF)
private val DarkAccent = Color(0xFFE5E5E5) // light gray — neutral selection tint

private val DarkBackground = Color(0xFF0F0F0F)
private val DarkSurfaceElevated = Color(0xFF1A1A1A)
private val DarkOnSurface = Color(0xFFEDEDED)
private val DarkOnSurfaceVariant = Color(0xFFBDBDBD)

// Light palette taken from iTunes Store / iOS system colors.
private val LightBackground = Color(0xFFFFFFFF)
private val LightSurfaceElevated = Color(0xFFF2F2F7) // iOS systemGray6
private val LightOnSurface = Color(0xFF000000)
private val LightOnSurfaceVariant = Color(0xFF8E8E93) // iOS systemGray
private val LightOutline = Color(0xFFC6C6C8)         // iOS separator
private val LightOutlineVariant = Color(0xFFE5E5EA)  // iOS systemGray5

private val DarkITunesScheme = darkColorScheme(
    // Selection is light gray on dark — readable, not loud.
    primary = DarkAccent,
    onPrimary = Color(0xFF0F0F0F),               // dark text/icon on light selected pill
    primaryContainer = Color(0xFF2A2A2A),        // subtle elevated container, not red tint
    onPrimaryContainer = DarkOnSurface,
    secondary = DarkAccent,
    onSecondary = Color(0xFF0F0F0F),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkBackground,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkOnSurfaceVariant,
    // Stepped tonal containers so the mini Now Playing card visibly lifts off the
    // background — the default M3 derivation barely differs from `surface` in our
    // near-black scheme.
    surfaceContainer = Color(0xFF1F1F1F),
    surfaceContainerHigh = Color(0xFF2C2C2C),
    surfaceContainerHighest = Color(0xFF353535),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2A2A2A),
)

private val LightITunesScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E7FF),
    onPrimaryContainer = Color(0xFF003E7A),
    secondary = LightAccent,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightBackground,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = Color(0xFFF2F2F7),
    surfaceContainerHigh = Color(0xFFEBEBF0),
    surfaceContainerHighest = Color(0xFFE3E3E8),
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
)

@Composable
@ReadOnlyComposable
@Suppress("UNUSED_PARAMETER")
fun resolvePalette(palette: PaletteId, isDark: Boolean): ColorScheme =
    if (isDark) DarkITunesScheme else LightITunesScheme
