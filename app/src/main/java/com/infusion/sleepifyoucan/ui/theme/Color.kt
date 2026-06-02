package com.infusion.sleepifyoucan.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Canvas = Color(0xFF07080A)
val Surface = Color(0xFF0D0D0D)
val SurfaceElevated = Color(0xFF101111)
val SurfaceCard = Color(0xFF121212)
val ButtonForeground = Color(0xFF18191A)

val Hairline = Color(0xFF242728)
val HairlineSoft = Color.White.copy(alpha = 0.08f)
val HairlineStrong = Color.White.copy(alpha = 0.16f)

val Primary = Color.White
val PrimaryPressed = Color(0xFFE8E8E8)
val OnPrimary = Color.Black

val Ink = Color(0xFFF4F4F6)
val Body = Color(0xFFCDCDCD)
val CharcoalText = Color(0xFFD3D3D4)
val Mute = Color(0xFF9C9C9D)
val Ash = Color(0xFF6A6B6C)
val Stone = Color(0xFF434345)

val AccentBlue = Color(0xFF57C1FF)
val AccentBlueSoft = Color(0x2657C1FF)
val AccentRed = Color(0xFFFF6161)
val AccentRedSoft = Color(0x26FF6161)
val AccentGreen = Color(0xFF59D499)
val AccentGreenSoft = Color(0x2659D499)
val AccentYellow = Color(0xFFFFC533)
val AccentYellowSoft = Color(0x26FFC533)

val HeroStripeStart = Color(0xFFFF5757)
val HeroStripeEnd = Color(0xFFA1131A)

val GradientPrimary = Brush.verticalGradient(
    colors = listOf(Canvas, Surface, SurfaceElevated)
)

val GradientSecondary = Brush.verticalGradient(
    colors = listOf(SurfaceElevated, Canvas)
)

val GradientAccent = Brush.verticalGradient(
    colors = listOf(Surface, SurfaceElevated, Surface)
)

val GradientSuccess = Brush.verticalGradient(
    colors = listOf(Surface, AccentGreenSoft)
)

val GradientWarning = Brush.verticalGradient(
    colors = listOf(Surface, AccentYellowSoft)
)

val OnboardingGradient1 = GradientPrimary
val OnboardingGradient2 = GradientPrimary
val OnboardingGradient3 = GradientPrimary
val OnboardingGradient4 = GradientPrimary

val Terracotta = AccentBlue
val Sage = AccentGreen
val Amber = AccentYellow
val DustyBlue = AccentBlue
val SkyMist = AccentBlue
val DustyRose = AccentRed
val WarmIndigo = AccentBlue

val Charcoal = Canvas
val Espresso = Surface
val WarmBrown = SurfaceElevated
val WarmBlack = Canvas

val Parchment = SurfaceCard
val ParchmentDark = SurfaceElevated
val TerracottaLight = PrimaryPressed
val SageLight = AccentGreen

val TextPrimary = Ink
val TextSecondary = Body
val TextTertiary = Mute
val TextDisabled = Ash
val TextOnAccent = OnPrimary
val TextOnCard = Ink

val SketchCardBg = Surface
val SketchCardBorder = Hairline
val SketchCardHighlight = SurfaceElevated
val SketchCardOverlay = SurfaceCard

val Success = AccentGreen
val Warning = AccentYellow
val Error = AccentRed
val Info = AccentBlue

val BlackMute = Canvas
val BlackMuteDark = Canvas
val BlackMuteSurface = Surface
val PurpleNight = AccentBlue
val PurpleNightLight = AccentBlue
val OrangeJuice = AccentYellow
val OrangeJuiceLight = AccentYellowSoft
val YellowSand = AccentYellow
val YellowSandLight = AccentYellow
val GreenLand = AccentGreen
val GreenLandLight = AccentGreen
val DeepNavy = Canvas
val DarkBlue = Surface
val OceanBlue = SurfaceElevated
val Midnight = Canvas
val NavyLight = SurfaceElevated
val BlueLight = Hairline
val Coral = AccentBlue
val CoralLight = AccentBlueSoft
val Lavender = AccentBlue
val SkyBlue = AccentBlue
val OrangeAccent = AccentYellow
val ElectricBlue = AccentBlue
val Gold = AccentYellow
val Mint = AccentGreen
val MintLight = AccentGreen

val GlassWhite = SurfaceElevated
val GlassBorder = Hairline
val GlassHighlight = SurfaceCard
val GlassOverlay = SurfaceCard

val Clear = Color.Transparent
val SketchShadow = Color.Transparent
val SketchShadowAmbient = Color.Transparent
