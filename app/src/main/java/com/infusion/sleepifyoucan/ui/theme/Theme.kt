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

// Dark color scheme using our custom palette
// This app is dark-mode first since users open it in bed
private val SleepDarkColorScheme = darkColorScheme(
    primary = PurpleNight,
    onPrimary = TextPrimary,
    primaryContainer = PurpleNightLight,
    onPrimaryContainer = BlackMuteDark,
    
    secondary = YellowSand,
    onSecondary = BlackMuteDark,
    secondaryContainer = YellowSandLight,
    onSecondaryContainer = BlackMuteDark,
    
    tertiary = OrangeJuice,
    onTertiary = TextPrimary,
    tertiaryContainer = OrangeJuiceLight,
    onTertiaryContainer = BlackMuteDark,
    
    error = OrangeJuice,
    onError = TextPrimary,
    
    background = BlackMute,
    onBackground = TextPrimary,
    
    surface = BlackMuteSurface,
    onSurface = TextPrimary,
    surfaceVariant = BlackMuteDark,
    onSurfaceVariant = TextSecondary,
    
    outline = TextSecondary,
    outlineVariant = TextDisabled,
    
    inverseSurface = TextPrimary,
    inverseOnSurface = BlackMuteDark,
    inversePrimary = PurpleNight
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