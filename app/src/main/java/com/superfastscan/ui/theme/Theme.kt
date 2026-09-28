package com.superfastscan.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Blue60,
    onPrimary = Neutral04,
    primaryContainer = Blue20,
    onPrimaryContainer = Blue80,
    secondary = Indigo40,
    onSecondary = Neutral04,
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Indigo80,
    tertiary = SuccessGreen,
    background = Color.Transparent,
    onBackground = Neutral90,
    surface = SurfaceDark,
    onSurface = Neutral90,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = Neutral80,
    surfaceContainerLow = Neutral06,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    error = ErrorRed,
    onError = Neutral04,
    outline = Neutral30,
    outlineVariant = Neutral20
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue80,
    onPrimaryContainer = Blue20,
    secondary = Indigo40,
    onSecondary = Color.White,
    secondaryContainer = Indigo80,
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = SuccessGreen,
    background = Neutral99,
    onBackground = Neutral10,
    surface = SurfaceLight,
    onSurface = Neutral10,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = Neutral30,
    surfaceContainerLow = Neutral99,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    error = ErrorRedDark,
    onError = Color.White,
    outline = Neutral80,
    outlineVariant = Neutral90
)

@Composable
fun SuperFastScanTheme(
    darkTheme: Boolean = true, // Force dark mode aesthetic by default
    dynamicColor: Boolean = false, // Disable dynamic color to retain deep navy/blue aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
