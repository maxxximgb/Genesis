package dev.maxxximgb.genesis.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import dev.maxxximgb.genesis.R

/**
 * Default app font.
 *
 * The user asked for "SF Pro" — Apple's SF Pro EULA restricts redistribution to Apple
 * platforms, so shipping it inside an Android APK isn't legitimate. Instead we use Inter
 * (SIL OFL 1.1, free for any use), which was designed as a UI replacement for SF Pro and
 * is visually almost indistinguishable in titles and body sizes.
 *
 * Inter is fetched at runtime via the Google Fonts content provider (Play Services). The
 * provider caches the font, so subsequent launches are offline-safe.
 */
private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val Inter = GoogleFont("Inter")

private val InterFamily = FontFamily(
    Font(googleFont = Inter, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = Inter, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = Inter, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = Inter, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold),
)

/**
 * iTunes-style tightening over Material3 defaults: Inter has noticeably negative tracking
 * on display/title sizes when used in iOS-style UIs; pulling body letterSpacing toward
 * zero makes info-rich rows feel dense.
 */
val GenesisTypography: Typography = Typography().run {
    val titleTight = (-0.3).sp
    val bodyTight = 0.sp
    fun TextStyle.withFamily(): TextStyle = copy(fontFamily = InterFamily)
    copy(
        displayLarge = displayLarge.withFamily().copy(letterSpacing = (-0.5).sp),
        displayMedium = displayMedium.withFamily().copy(letterSpacing = (-0.4).sp),
        displaySmall = displaySmall.withFamily().copy(letterSpacing = (-0.3).sp),
        headlineLarge = headlineLarge.withFamily().copy(letterSpacing = (-0.4).sp),
        headlineMedium = headlineMedium.withFamily().copy(letterSpacing = (-0.3).sp),
        headlineSmall = headlineSmall.withFamily().copy(letterSpacing = (-0.2).sp),
        titleLarge = titleLarge.withFamily().copy(letterSpacing = titleTight),
        titleMedium = titleMedium.withFamily().copy(letterSpacing = titleTight),
        titleSmall = titleSmall.withFamily().copy(letterSpacing = (-0.2).sp),
        bodyLarge = bodyLarge.withFamily().copy(letterSpacing = bodyTight),
        bodyMedium = bodyMedium.withFamily().copy(letterSpacing = bodyTight, lineHeight = 18.sp),
        bodySmall = bodySmall.withFamily().copy(letterSpacing = bodyTight, lineHeight = 16.sp),
        labelLarge = labelLarge.withFamily().copy(letterSpacing = 0.sp),
        labelMedium = labelMedium.withFamily(),
        labelSmall = labelSmall.withFamily(),
    )
}
