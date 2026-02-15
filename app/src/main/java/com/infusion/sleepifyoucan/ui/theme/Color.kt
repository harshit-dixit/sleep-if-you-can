package com.infusion.sleepifyoucan.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Modern Gradient Backgrounds ──────────────────────────────────────────────

val GradientPrimary = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0D0D1A), // Deep space
        Color(0xFF1A1A2E), // Deep navy
        Color(0xFF16213E)  // Dark blue
    )
)

val GradientSecondary = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF16213E),
        Color(0xFF0D0D1A),
    )
)

val GradientAccent = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1A1A2E),
        Color(0xFF2D1B3D), // Deep purple tint
        Color(0xFF1A1A2E)
    )
)

val GradientSuccess = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0D2818),
        Color(0xFF1A3A2A),
    )
)

val GradientWarning = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF2D2410),
        Color(0xFF3D3018),
    )
)

// ── Onboarding Gradients (high contrast, dark base for white text) ──────────

val OnboardingGradient1 = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0D0D1A),
        Color(0xFF1B1040),
        Color(0xFF2D1663)
    )
)

val OnboardingGradient2 = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0D0D1A),
        Color(0xFF102040),
        Color(0xFF0F3460)
    )
)

val OnboardingGradient3 = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0D0D1A),
        Color(0xFF1A2030),
        Color(0xFF203040)
    )
)

val OnboardingGradient4 = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0D0D1A),
        Color(0xFF1B1040),
        Color(0xFF0F3460)
    )
)

// ── Vibrant Solid Colors ─────────────────────────────────────────────────────

val Coral = Color(0xFFE94560)         // Primary accent - energetic
val Mint = Color(0xFF00D4AA)          // Success - fresh, calming
val Gold = Color(0xFFFFD700)          // Achievement - rewarding
val Lavender = Color(0xFFB794F6)      // Secondary accent - creative
val SkyBlue = Color(0xFF87CEEB)       // Calm - peaceful
val OrangeAccent = Color(0xFFFF8E53)  // Orange Accent
val ElectricBlue = Color(0xFF6C63FF)  // Electric accent

// ── Dark Theme Backgrounds (Modern) ──────────────────────────────────────────

val DeepNavy = Color(0xFF0D0D1A)       // Primary background
val DarkBlue = Color(0xFF161625)       // Surface elevation
val OceanBlue = Color(0xFF0F3460)      // Feature accent
val Midnight = Color(0xFF080810)       // Deepest backgrounds

// ── Light Variants for Cards and Surfaces ────────────────────────────────────

val NavyLight = Color(0xFF1E1E35)      // Elevated surfaces
val BlueLight = Color(0xFF253E5E)      // Hover states
val CoralLight = Color(0xFFEB5668)     // Button pressed states
val MintLight = Color(0xFF26D6B8)      // Success light

// ── Text Colors (High Contrast) ──────────────────────────────────────────────

val TextPrimary = Color(0xFFF0F0F5)     // Bright white for primary text
val TextSecondary = Color(0xFFB0BEC5)   // Soft blue-gray
val TextTertiary = Color(0xFF8892A0)    // Muted for tertiary text
val TextDisabled = Color(0xFF5A6474)    // Disabled state
val TextOnAccent = Color(0xFFFFFFFF)    // Pure white on colored backgrounds

// ── Glassmorphism Colors ─────────────────────────────────────────────────────

val GlassWhite = Color(0x18FFFFFF)     // 9% white for glass background
val GlassBorder = Color(0x30FFFFFF)    // 19% white for glass borders
val GlassHighlight = Color(0x10FFFFFF) // 6% white for subtle highlights
val GlassOverlay = Color(0x08FFFFFF)   // 3% white for faint overlay

// ── Status Colors ────────────────────────────────────────────────────────────

val Success = Color(0xFF00D4AA)
val Warning = Color(0xFFFFD700)
val Error = Color(0xFFE94560)
val Info = Color(0xFF87CEEB)

// ── Legacy Aliases (backward compatibility) ──────────────────────────────────

val BlackMute = DeepNavy
val BlackMuteDark = Midnight
val BlackMuteSurface = DarkBlue
val PurpleNight = Lavender
val PurpleNightLight = Color(0xFFC4B5FD)
val OrangeJuice = Coral
val OrangeJuiceLight = CoralLight
val YellowSand = Gold
val YellowSandLight = Color(0xFFFFE066)
val GreenLand = Mint
val GreenLandLight = MintLight