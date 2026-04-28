package dev.maxxximgb.genesis.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

enum class PaletteId(val seed: Color, val displayResId: Int) {
    VERDANT(Color(0xFF1DB954), dev.maxxximgb.genesis.R.string.palette_verdant),
    AMBER(Color(0xFFB45309), dev.maxxximgb.genesis.R.string.palette_amber),
    INDIGO(Color(0xFF3F51B5), dev.maxxximgb.genesis.R.string.palette_indigo),
    CRIMSON(Color(0xFFDC2626), dev.maxxximgb.genesis.R.string.palette_crimson),
    TEAL(Color(0xFF0F766E), dev.maxxximgb.genesis.R.string.palette_teal),
    DYNAMIC(Color(0xFF1DB954), dev.maxxximgb.genesis.R.string.palette_dynamic);

    companion object {
        val DEFAULT = VERDANT

        fun fromName(name: String?): PaletteId =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

@Composable
@ReadOnlyComposable
fun resolvePalette(palette: PaletteId, isDark: Boolean): ColorScheme {
    if (palette == PaletteId.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        return if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    val seed = if (palette == PaletteId.DYNAMIC) PaletteId.DEFAULT.seed else palette.seed
    return dynamicColorScheme(
        seedColor = seed,
        isDark = isDark,
        isAmoled = false,
        style = PaletteStyle.TonalSpot,
    )
}
