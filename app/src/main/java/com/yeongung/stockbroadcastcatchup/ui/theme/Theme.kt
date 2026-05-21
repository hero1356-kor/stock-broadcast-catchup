package com.yeongung.stockbroadcastcatchup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object CatchupColors {
    val Page = Color(0xFF071016)
    val Surface = Color(0xFF101B24)
    val SurfaceRaised = Color(0xFF142231)
    val SurfaceMuted = Color(0xFF1D2B38)
    val Ink = Color(0xFFF4F8FB)
    val InkMuted = Color(0xFFA8B5C2)
    val Primary = Color(0xFF20C7BE)
    val PrimarySoft = Color(0xFF103C42)
    val Secondary = Color(0xFF4EA5FF)
    val Danger = Color(0xFFFF7A7A)
    val Warning = Color(0xFFFFD166)
    val Positive = Color(0xFF5CE1A8)
}

private val CatchupColorScheme = darkColorScheme(
    primary = CatchupColors.Primary,
    secondary = CatchupColors.Secondary,
    background = CatchupColors.Page,
    surface = CatchupColors.Surface,
    onPrimary = Color(0xFF041313),
    onSecondary = Color.White,
    onBackground = CatchupColors.Ink,
    onSurface = CatchupColors.Ink,
)

private val CatchupTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 30.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 25.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
)

@Composable
fun StockBroadcastCatchupTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CatchupColorScheme,
        typography = CatchupTypography,
        content = content,
    )
}
