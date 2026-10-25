package net.sentinelsafeguard.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The Sentinel black/green system (spec §8): base black #060806,
 * Sentinel green accents, fleet danger red — and no purple anywhere.
 * Dark-only in v1. Every text pairing below clears the 4.5:1 floor:
 *  - body text #E6EDE6 on #060806  (≈ 13:1)
 *  - muted text #9DB89D on #060806 (≈ 7:1)
 *  - green #00E676 on #060806      (≈ 9:1)
 *  - black text on green buttons   (≈ 9:1)
 *  - white on danger red #B03A2E   (≈ 5.9:1)
 */
val SentinelBlack = Color(0xFF060806)
val SentinelSurface = Color(0xFF0D120D)
val SentinelSurfaceHigh = Color(0xFF162016)
val SentinelGreen = Color(0xFF00E676)
val SentinelGreenDim = Color(0xFF00B248)
val SentinelText = Color(0xFFE6EDE6)
val SentinelMuted = Color(0xFF9DB89D)
val SentinelDanger = Color(0xFFB03A2E)
val SentinelDangerText = Color(0xFFFFFFFF)
val SentinelOutline = Color(0xFF2A3A2A)

private val SentinelColors = darkColorScheme(
    primary = SentinelGreen,
    onPrimary = Color(0xFF031007),
    secondary = SentinelGreenDim,
    onSecondary = Color(0xFF031007),
    background = SentinelBlack,
    onBackground = SentinelText,
    surface = SentinelSurface,
    onSurface = SentinelText,
    surfaceVariant = SentinelSurfaceHigh,
    onSurfaceVariant = SentinelMuted,
    error = SentinelDanger,
    onError = SentinelDangerText,
    outline = SentinelOutline,
)

private val SentinelTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
    ),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
    ),
)

@Composable
fun SentinelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SentinelColors,
        typography = SentinelTypography,
        content = content,
    )
}
