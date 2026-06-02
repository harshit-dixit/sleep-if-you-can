package com.infusion.sleepifyoucan.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SleepDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = Ink,
    secondary = Body,
    onSecondary = Canvas,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = Ink,
    tertiary = AccentGreen,
    onTertiary = Canvas,
    tertiaryContainer = AccentGreenSoft,
    onTertiaryContainer = AccentGreen,
    error = AccentRed,
    onError = Canvas,
    errorContainer = AccentRedSoft,
    onErrorContainer = AccentRed,
    background = Canvas,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = Body,
    surfaceTint = Color.Transparent,
    outline = Hairline,
    outlineVariant = HairlineSoft,
    inverseSurface = Primary,
    inverseOnSurface = OnPrimary,
    inversePrimary = PrimaryPressed,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceCard,
    surfaceContainerHighest = ButtonForeground,
    surfaceContainerLow = Surface,
    surfaceContainerLowest = Canvas,
    scrim = Color.Black
)

@Composable
fun SleepIfYouCanTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Canvas.toArgb()
            window.navigationBarColor = Canvas.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = SleepDarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
