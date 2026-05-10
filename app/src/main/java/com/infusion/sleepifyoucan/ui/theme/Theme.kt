package com.infusion.sleepifyoucan.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Warm sketch-inspired dark color scheme
private val SleepDarkColorScheme = darkColorScheme(
    primary = Terracotta,
    onPrimary = TextOnAccent,
    primaryContainer = Color(0xFF453A33),
    onPrimaryContainer = TextPrimary,

    secondary = DustyBlue,
    onSecondary = TextOnAccent,
    secondaryContainer = Espresso,
    onSecondaryContainer = TextPrimary,

    tertiary = Sage,
    onTertiary = TextOnAccent,
    tertiaryContainer = WarmBrown,
    onTertiaryContainer = TextPrimary,

    error = Error,
    onError = TextOnAccent,
    errorContainer = Color(0xFF3D2222),
    onErrorContainer = Error,

    background = Charcoal,
    onBackground = TextPrimary,

    surface = Espresso,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF453A33),
    onSurfaceVariant = TextSecondary,

    surfaceTint = Terracotta,

    outline = TextTertiary,
    outlineVariant = TextDisabled,

    inverseSurface = TextPrimary,
    inverseOnSurface = Charcoal,
    inversePrimary = Terracotta,

    surfaceContainer = WarmBrown,
    surfaceContainerHigh = Color(0xFF453A33),
    surfaceContainerHighest = Color(0xFF5A4E44),
    surfaceContainerLow = Espresso,
    surfaceContainerLowest = WarmBlack,

    scrim = Color(0xFF000000)
)

@Composable
fun SleepIfYouCanTheme(
    // Always use dark theme - users open this app in bed
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = SleepDarkColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = WarmBlack.toArgb()
            window.navigationBarColor = WarmBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}