package com.infusion.sleepifyoucan.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism card with frosted glass effect.
 * Uses translucent white background with subtle border for depth.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = GlassWhite,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .padding(16.dp),
        content = content
    )
}

/**
 * Glassmorphism surface — full width frosted surface for sections.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GlassWhite,
                        GlassHighlight
                    )
                )
            )
            .border(1.dp, GlassBorder, shape)
            .padding(16.dp),
        content = content
    )
}

/**
 * Modifier extension for applying glassmorphism to any composable.
 */
fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = GlassWhite,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(borderWidth, borderColor, shape)

/**
 * Glassmorphism navigation bar modifier.
 */
fun Modifier.glassNavBar(): Modifier = this
    .background(
        Brush.verticalGradient(
            colors = listOf(
                Color(0x20FFFFFF),
                Color(0x10FFFFFF)
            )
        )
    )
    .border(
        width = 0.5.dp,
        color = GlassBorder,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    )

/**
 * Accent-highlighted glass card (for selected states).
 */
@Composable
fun GlassCardAccent(
    modifier: Modifier = Modifier,
    accentColor: Color = Coral,
    isSelected: Boolean = false,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.15f) else GlassWhite
    val border = if (isSelected) accentColor.copy(alpha = 0.6f) else GlassBorder

    Column(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, border, shape)
            .padding(12.dp),
        content = content
    )
}
