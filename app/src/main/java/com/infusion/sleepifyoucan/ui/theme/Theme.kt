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

// Enhanced dark color scheme with modern colors
private val SleepDarkColorScheme = darkColorScheme(
    primary = Coral,
    onPrimary = TextPrimary,
    primaryContainer = NavyLight,
    onPrimaryContainer = TextPrimary,

    secondary = Lavender,
    onSecondary = TextPrimary,
    secondaryContainer = DarkBlue,
    onSecondaryContainer = TextPrimary,

    tertiary = Mint,
    onTertiary = TextPrimary,
    tertiaryContainer = OceanBlue,
    onTertiaryContainer = TextPrimary,

    error = Error,
    onError = TextPrimary,
    errorContainer = Color(0xFF2D1B1B),
    onErrorContainer = Error,

    background = DeepNavy,
    onBackground = TextPrimary,

    surface = DarkBlue,
    onSurface = TextPrimary,
    surfaceVariant = NavyLight,
    onSurfaceVariant = TextSecondary,

    surfaceTint = Coral,

    outline = TextTertiary,
    outlineVariant = TextDisabled,

    inverseSurface = TextPrimary,
    inverseOnSurface = DeepNavy,
    inversePrimary = Coral,

    surfaceContainer = OceanBlue,
    surfaceContainerHigh = NavyLight,
    surfaceContainerHighest = BlueLight,
    surfaceContainerLow = DarkBlue,
    surfaceContainerLowest = Midnight,

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
            window.statusBarColor = BlackMuteDark.toArgb()
            window.navigationBarColor = BlackMuteDark.toArgb()
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