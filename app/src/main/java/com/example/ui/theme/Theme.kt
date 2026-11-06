package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CallMasterDarkColorScheme = darkColorScheme(
    primary = PlatinumPrimary,
    onPrimary = CarbonDark,
    primaryContainer = ElevatedGrayDark,
    onPrimaryContainer = SilverPure,
    secondary = SteelSurfaceDark,
    onSecondary = SilverPure,
    secondaryContainer = GraphiteDark,
    onSecondaryContainer = PlatinumPrimary,
    tertiary = MutedSlate,
    onTertiary = CarbonDark,
    background = CarbonDark,
    onBackground = SilverPure,
    surface = GraphiteDark,
    onSurface = SilverPure,
    surfaceVariant = SteelSurfaceDark,
    onSurfaceVariant = MutedSlate,
    outline = BorderGrayDark,
    error = StatusError,
    onError = Color.White
)

val CallMasterLightColorScheme = lightColorScheme(
    primary = Color(0xFF1F242D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E7EB),
    onPrimaryContainer = Color(0xFF111827),
    secondary = Color(0xFF4B5563),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3F4F6),
    onSecondaryContainer = Color(0xFF1F242D),
    tertiary = Color(0xFF6B7280),
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = BorderGrayLight,
    error = StatusError,
    onError = Color.White
)

@Composable
fun CallMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CallMasterDarkColorScheme else CallMasterLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CallMasterTypography,
        content = content
    )
}
