package com.infusion.sleepifyoucan.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    backgroundColor: Color = SketchCardBg,
    borderColor: Color = SketchCardBorder,
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

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(SketchCardBg, SketchCardHighlight)
                )
            )
            .border(1.dp, SketchCardBorder, shape)
            .padding(16.dp),
        content = content
    )
}

fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(10.dp),
    backgroundColor: Color = SketchCardBg,
    borderColor: Color = SketchCardBorder,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(borderWidth, borderColor, shape)

fun Modifier.glassNavBar(): Modifier = this
    .background(Canvas)
    .border(
        width = 1.dp,
        color = Hairline,
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
    )

@Composable
fun GlassCardAccent(
    modifier: Modifier = Modifier,
    accentColor: Color = Terracotta,
    isSelected: Boolean = false,
    shape: Shape = RoundedCornerShape(10.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val background = if (isSelected) SurfaceElevated else SketchCardBg
    val border = if (isSelected) accentColor.copy(alpha = 0.35f) else SketchCardBorder

    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .padding(12.dp),
        content = content
    )
}
