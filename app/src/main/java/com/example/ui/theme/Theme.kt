package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CallMasterDarkColorScheme = darkColorScheme(
    primary = RadiantGold,
    onPrimary = DarkGreen,
    primaryContainer = PetroleumGreen,
    onPrimaryContainer = LightGold,
    secondary = LightGreen,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = OnSurfaceDark,
    tertiary = LightGold,
    onTertiary = DarkGold,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = StatusError,
    onError = Color.White
)

val CallMasterLightColorScheme = lightColorScheme(
    primary = PetroleumGreen,
    onPrimary = Color.White,
    primaryContainer = SurfaceVariantLight,
    onPrimaryContainer = DarkGreen,
    secondary = RadiantGold,
    onSecondary = DarkGreen,
    secondaryContainer = LightGold,
    onSecondaryContainer = DarkGold,
    tertiary = LightGreen,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
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
