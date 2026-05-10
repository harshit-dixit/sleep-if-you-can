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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Warm paper-style card — replaces the old glass/frost effect.
 * Uses a solid warm background with subtle border and soft shadow for depth.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = SketchCardBg,
    borderColor: Color = SketchCardBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(4.dp, shape, ambientColor = SketchShadowAmbient, spotColor = SketchShadow)
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .padding(16.dp),
        content = content
    )
}

/**
 * Warm paper surface — full width section container.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(2.dp, shape, ambientColor = SketchShadowAmbient, spotColor = SketchShadow)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SketchCardBg,
                        SketchCardHighlight
                    )
                )
            )
            .border(1.dp, SketchCardBorder, shape)
            .padding(16.dp),
        content = content
    )
}

/**
 * Modifier extension for applying warm paper style to any composable.
 */
fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = SketchCardBg,
    borderColor: Color = SketchCardBorder,
    borderWidth: Dp = 1.dp
): Modifier = this
    .shadow(4.dp, shape, ambientColor = SketchShadowAmbient, spotColor = SketchShadow)
    .clip(shape)
    .background(backgroundColor)
    .border(borderWidth, borderColor, shape)

/**
 * Warm navigation bar modifier.
 */
fun Modifier.glassNavBar(): Modifier = this
    .background(
        Brush.verticalGradient(
            colors = listOf(
                Color(0xE02C2520),
                Color(0xD01E1A17)
            )
        )
    )
    .border(
        width = 0.5.dp,
        color = SketchCardBorder,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    )

/**
 * Accent-highlighted warm card (for selected states).
 */
@Composable
fun GlassCardAccent(
    modifier: Modifier = Modifier,
    accentColor: Color = Terracotta,
    isSelected: Boolean = false,
    shape: Shape = RoundedCornerShape(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.15f) else SketchCardBg
    val border = if (isSelected) accentColor.copy(alpha = 0.6f) else SketchCardBorder

    Column(
        modifier = modifier
            .shadow(
                if (isSelected) 6.dp else 2.dp,
                shape,
                ambientColor = if (isSelected) accentColor.copy(alpha = 0.1f) else SketchShadowAmbient,
                spotColor = SketchShadow
            )
            .clip(shape)
            .background(bgColor)
            .border(1.dp, border, shape)
            .padding(12.dp),
        content = content
    )
}
