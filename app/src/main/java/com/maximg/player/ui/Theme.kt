package com.maximg.player.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFFB45309),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFBD8A3),
    onPrimaryContainer = Color(0xFF3F2200),
    secondary = Color(0xFF0F766E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBCECE6),
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = Color(0xFF1D4ED8),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F4EE),
    onBackground = Color(0xFF1E1B16),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF1E1B16),
    surfaceVariant = Color(0xFFEDE4D5),
    onSurfaceVariant = Color(0xFF4B4539),
    outline = Color(0xFF7D7668),
    outlineVariant = Color(0xFFCEC5B7)
)

private val MorningTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

private val MorningShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun MorningPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MorningTypography,
        shapes = MorningShapes,
        content = content
    )
}
