package com.yeongung.stockbroadcastcatchup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object CatchupColors {
    val Page = Color(0xFFF6F7F9)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFECEFF3)
    val Ink = Color(0xFF14181F)
    val InkMuted = Color(0xFF667085)
    val Primary = Color(0xFF0F766E)
    val Secondary = Color(0xFF2457A6)
    val Danger = Color(0xFFB42318)
    val Warning = Color(0xFFB7791F)
    val Positive = Color(0xFF087443)
}

private val CatchupColorScheme = lightColorScheme(
    primary = CatchupColors.Primary,
    secondary = CatchupColors.Secondary,
    background = CatchupColors.Page,
    surface = CatchupColors.Surface,
    onPrimary = Color.White,
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
