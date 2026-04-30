package dev.maxxximgb.genesis.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

/**
 * iTunes-style tightening over Material3 defaults.
 *
 * SF Pro (Apple's system font) has noticeably negative tracking on display/title sizes;
 * Roboto on Android does not. We approximate by overriding letterSpacing on titles and
 * pulling body letterSpacing closer to zero so info-rich rows feel dense.
 */
val GenesisTypography: Typography = Typography().run {
    val titleTight = (-0.3).sp
    val bodyTight = 0.sp
    copy(
        displayLarge = displayLarge.copy(letterSpacing = (-0.5).sp),
        displayMedium = displayMedium.copy(letterSpacing = (-0.4).sp),
        displaySmall = displaySmall.copy(letterSpacing = (-0.3).sp),
        headlineLarge = headlineLarge.copy(letterSpacing = (-0.4).sp),
        headlineMedium = headlineMedium.copy(letterSpacing = (-0.3).sp),
        headlineSmall = headlineSmall.copy(letterSpacing = (-0.2).sp),
        titleLarge = titleLarge.copy(letterSpacing = titleTight),
        titleMedium = titleMedium.copy(letterSpacing = titleTight),
        titleSmall = titleSmall.copy(letterSpacing = (-0.2).sp),
        bodyLarge = bodyLarge.copy(letterSpacing = bodyTight),
        bodyMedium = bodyMedium.copy(letterSpacing = bodyTight, lineHeight = 18.sp),
        bodySmall = bodySmall.copy(letterSpacing = bodyTight, lineHeight = 16.sp),
        labelLarge = labelLarge.copy(letterSpacing = 0.sp),
    )
}
