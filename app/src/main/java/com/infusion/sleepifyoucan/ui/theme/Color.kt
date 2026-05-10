package com.infusion.sleepifyoucan.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Warm Sketch Backgrounds (Paper / Charcoal) ──────────────────────────────

val GradientPrimary = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1E1A17), // Warm black
        Color(0xFF2C2520), // Charcoal
        Color(0xFF3A302A)  // Espresso
    )
)

val GradientSecondary = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF3A302A),
        Color(0xFF1E1A17),
    )
)

val GradientAccent = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF2C2520),
        Color(0xFF3D2E28), // Warm brown tint
        Color(0xFF2C2520)
    )
)

val GradientSuccess = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1E2A1E),
        Color(0xFF2A3A28),
    )
)

val GradientWarning = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF2D2815),
        Color(0xFF3D3520),
    )
)

// ── Onboarding Gradients (warm dark tones for white text) ───────────────────

val OnboardingGradient1 = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1E1A17),
        Color(0xFF2E2420),
        Color(0xFF3D2E28)
    )
)

val OnboardingGradient2 = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1E1A17),
        Color(0xFF252820),
        Color(0xFF2D3528)
    )
)

val OnboardingGradient3 = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1E1A17),
        Color(0xFF28251E),
        Color(0xFF353028)
    )
)

val OnboardingGradient4 = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1E1A17),
        Color(0xFF2E2420),
        Color(0xFF2D3528)
    )
)

// ── Warm Solid Colors (Sketch Palette) ──────────────────────────────────────

val Terracotta = Color(0xFFC67B5C)       // Primary accent — warm, hand-crafted
val Sage = Color(0xFF7D9B76)             // Success — natural, earthy
val Amber = Color(0xFFD4A853)            // Achievement — warm glow
val DustyBlue = Color(0xFF7B94A8)        // Secondary accent — calm, muted
val SkyMist = Color(0xFFA8BCC8)          // Calm — soft, airy
val DustyRose = Color(0xFFC25B56)        // Urgency — warm alarm
val WarmIndigo = Color(0xFF7B6FA0)       // Highlight accent

// ── Dark Theme Backgrounds (Warm Charcoal) ──────────────────────────────────

val Charcoal = Color(0xFF2C2520)           // Primary background
val Espresso = Color(0xFF3A302A)           // Surface elevation
val WarmBrown = Color(0xFF4A3E36)          // Feature accent
val WarmBlack = Color(0xFF1E1A17)          // Deepest backgrounds

// ── Light Variants for Cards and Surfaces ────────────────────────────────────

val Parchment = Color(0xFFF5ECD7)          // Card / paper surface
val ParchmentDark = Color(0xFFE8DCC4)      // Pressed / hovered cards
val TerracottaLight = Color(0xFFD48E72)    // Button pressed states
val SageLight = Color(0xFF8DAA86)          // Success light

// ── Text Colors (Warm Contrast) ─────────────────────────────────────────────

val TextPrimary = Color(0xFFF2E8D5)         // Warm cream on dark backgrounds
val TextSecondary = Color(0xFFB0A898)        // Warm gray
val TextTertiary = Color(0xFF8A7E72)         // Muted warm gray
val TextDisabled = Color(0xFF6A5E54)         // Disabled state
val TextOnAccent = Color(0xFFFFFBF5)         // Near-white on colored backgrounds
val TextOnCard = Color(0xFF3B3029)           // Dark brown text on parchment cards

// ── Sketch Card Colors ──────────────────────────────────────────────────────

val SketchCardBg = Color(0xFF3A302A)         // Card background (dark mode)
val SketchCardBorder = Color(0xFF5A4E44)     // Subtle warm border
val SketchCardHighlight = Color(0xFF4A3E36)  // Faint highlight
val SketchCardOverlay = Color(0xFF332A24)    // Overlay tint

// ── Status Colors ────────────────────────────────────────────────────────────

val Success = Color(0xFF7D9B76)
val Warning = Color(0xFFD4A853)
val Error = Color(0xFFC25B56)
val Info = Color(0xFF7B94A8)

// ── Legacy Aliases (backward compatibility during migration) ─────────────────

val BlackMute = Charcoal
val BlackMuteDark = WarmBlack
val BlackMuteSurface = Espresso
val PurpleNight = DustyBlue
val PurpleNightLight = SkyMist
val OrangeJuice = Terracotta
val OrangeJuiceLight = TerracottaLight
val YellowSand = Amber
val YellowSandLight = Color(0xFFDEB567)
val GreenLand = Sage
val GreenLandLight = SageLight
val DeepNavy = Charcoal
val DarkBlue = Espresso
val OceanBlue = WarmBrown
val Midnight = WarmBlack
val NavyLight = Color(0xFF453A33)
val BlueLight = Color(0xFF5A4E44)
val Coral = Terracotta
val CoralLight = TerracottaLight
val Lavender = DustyBlue
val SkyBlue = SkyMist
val OrangeAccent = Color(0xFFD08B5B)
val ElectricBlue = WarmIndigo
val Gold = Amber
val Mint = Sage
val MintLight = SageLight

// ── Glass aliases → Sketch (keep GlassXxx names so existing code compiles) ──

val GlassWhite = SketchCardBg
val GlassBorder = SketchCardBorder
val GlassHighlight = SketchCardHighlight
val GlassOverlay = SketchCardOverlay

// ── Additional Utilities ─────────────────
val Clear = Color.Transparent
val SketchShadow = Color(0x15000000)
val SketchShadowAmbient = Color(0x10000000)