package com.infusion.sleepifyoucan.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Modern Gradient Backgrounds
val GradientPrimary = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1A1A2E), // Deep navy
        Color(0xFF16213E), // Dark blue
        Color(0xFF0F3460)  // Ocean blue
    )
)

val GradientSecondary = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F3460), // Ocean blue
        Color(0xFF1A1A2E), // Deep navy
    )
)

val GradientAccent = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFE94560), // Coral red
        Color(0xFFFF6B6B), // Light coral
        Color(0xFFFF8E53)  // Orange
    )
)

val GradientSuccess = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF00D4AA), // Mint
        Color(0xFF00E676), // Green
    )
)

val GradientWarning = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFFFA500), // Orange
    )
)

// Vibrant Solid Colors
val Coral = Color(0xFFE94560)         // Primary accent - energetic, attention-grabbing
val Mint = Color(0xFF00D4AA)          // Success - fresh, calming
val Gold = Color(0xFFFFD700)          // Achievement - rewarding, celebratory
val Lavender = Color(0xFFB794F6)      // Secondary accent - creative, modern
val SkyBlue = Color(0xFF87CEEB)       // Calm - peaceful, trustworthy
val OrangeAccent = Color(0xFFFF8E53)  // Orange Accent

// Dark Theme Backgrounds (Modern)
val DeepNavy = Color(0xFF1A1A2E)       // Primary background - sophisticated
val DarkBlue = Color(0xFF16213E)       // Surface elevation
val OceanBlue = Color(0xFF0F3460)      // Cards and components
val Midnight = Color(0xFF0A0A0F)       // Deepest backgrounds

// Light Variants for Cards and Surfaces
val NavyLight = Color(0xFF2A2A4E)      // Elevated surfaces
val BlueLight = Color(0xFF253E5E)      // Hover states
val CoralLight = Color(0xFFEB5668)     // Button pressed states
val MintLight = Color(0xFF26D6B8)      // Success light

// Text Colors (Improved Contrast)
val TextPrimary = Color(0xFFF8F9FA)     // Pure white for primary text
val TextSecondary = Color(0xFFB8C5D6)   // Soft blue-gray for secondary
val TextTertiary = Color(0xFF8892A0)    // Muted for tertiary text
val TextDisabled = Color(0xFF5A6474)    // Disabled state

// Status Colors
val Success = Color(0xFF00D4AA)         // Success states
val Warning = Color(0xFFFFD700)         // Warning states
val Error = Color(0xFFE94560)           // Error states
val Info = Color(0xFF87CEEB)            // Info states

// Legacy Colors (for backward compatibility)
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