package com.infusion.sleepifyoucan.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infusion.sleepifyoucan.ui.theme.*
import com.infusion.sleepifyoucan.utils.ShakeDetector
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipPath

@Composable
fun ShakeMissionScreen(
    currentShakes: Int,
    targetShakes: Int,
    shakeDetector: ShakeDetector,
    onShake: () -> Unit
) {
    var shakeTriggered by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        shakeDetector.start {
            onShake()
            shakeTriggered = true
        }
        onDispose {
            shakeDetector.stop()
        }
    }
    
    // Auto-reset trigger for animation
    LaunchedEffect(shakeTriggered) {
        if (shakeTriggered) {
            kotlinx.coroutines.delay(300)
            shakeTriggered = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GradientPrimary)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated title
            ScaleFadeAnimation(visible = true) {
                Text(
                    text = "Shake to Dismiss!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Terracotta,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instruction text with breathing animation
            BreathingAnimation {
                Text(
                    text = "Shake your phone vigorously",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Progress indicator with laboratory beaker flask custom drawing
            val progress = currentShakes.toFloat() / targetShakes.toFloat()
            val sloshDirection = remember(currentShakes) { if (Math.random() < 0.5) 1f else -1f }
            val targetAngle = if (shakeTriggered) 18f * sloshDirection else 0f
            val wobbleAngle by animateFloatAsState(
                targetValue = targetAngle,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "wobble"
            )
            
            val infiniteTransition = rememberInfiniteTransition(label = "bubbles")
            val bubblePhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "bubbles"
            )

            BounceAnimation(isPressed = shakeTriggered) {
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        
                        val scaleX = width / 200f
                        val scaleY = height / 200f
                        
                        val flaskPath = Path().apply {
                            moveTo(75f * scaleX, 30f * scaleY) // Top-left lip
                            lineTo(125f * scaleX, 30f * scaleY) // Top-right lip
                            lineTo(125f * scaleX, 40f * scaleY) // Neck top-right
                            lineTo(120f * scaleX, 80f * scaleY) // Neck bottom-right
                            lineTo(170f * scaleX, 170f * scaleY) // Bottom-right corner
                            quadraticTo(175f * scaleX, 180f * scaleY, 160f * scaleX, 180f * scaleY) // Bottom-right curve
                            lineTo(40f * scaleX, 180f * scaleY) // Bottom line
                            quadraticTo(25f * scaleX, 180f * scaleY, 30f * scaleX, 170f * scaleY) // Bottom-left curve
                            lineTo(80f * scaleX, 80f * scaleY) // Neck bottom-left
                            lineTo(75f * scaleX, 40f * scaleY) // Neck top-left
                            close()
                        }
                        
                        clipPath(flaskPath) {
                            drawRect(color = WarmBlack.copy(alpha = 0.4f))
                            
                            // Bubble rendering
                            val bubbleOffsets = listOf(0.0f, 0.2f, 0.4f, 0.6f, 0.8f)
                            val bubbleXPercentages = listOf(0.4f, 0.6f, 0.5f, 0.35f, 0.65f)
                            val bubbleSizes = listOf(4f, 6f, 5f, 7f, 4f)
                            
                            bubbleOffsets.forEachIndexed { i, offsetValue ->
                                val currentBubbleProgress = (bubblePhase + offsetValue) % 1f
                                val liquidTopY = height - (150f * scaleY * progress)
                                val startY = height - 10f * scaleY
                                val currentY = startY - ((startY - liquidTopY) * currentBubbleProgress)
                                
                                if (currentY > liquidTopY && progress > 0f) {
                                    drawCircle(
                                        color = SkyMist.copy(alpha = 0.5f),
                                        radius = bubbleSizes[i] * scaleX,
                                        center = Offset(width * bubbleXPercentages[i], currentY)
                                    )
                                }
                            }
                            
                            // Liquid rendering
                            if (progress > 0f) {
                                val liquidPath = Path().apply {
                                    val liquidHeight = 150f * scaleY * progress
                                    val topY = height - liquidHeight
                                    
                                    moveTo(0f, height)
                                    lineTo(width, height)
                                    lineTo(width, topY)
                                    
                                    val sloshAmplitude = 15f * scaleY * (1f - progress.coerceIn(0f, 1f))
                                    val wobbleRadians = Math.toRadians(wobbleAngle.toDouble()).toFloat()
                                    val dy = sloshAmplitude * kotlin.math.sin(wobbleRadians)
                                    
                                    lineTo(width, topY + dy)
                                    lineTo(0f, topY - dy)
                                    close()
                                }
                                
                                val liquidGradient = Brush.verticalGradient(
                                    colors = listOf(
                                        Terracotta,
                                        DustyRose
                                    )
                                )
                                drawPath(liquidPath, liquidGradient)
                            }
                        }
                        
                        // Outline
                        drawPath(
                            path = flaskPath,
                            color = SketchCardBorder,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 4f * scaleX,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                        
                        // Lip line at top
                        drawLine(
                            color = SketchCardBorder,
                            start = Offset(70f * scaleX, 30f * scaleY),
                            end = Offset(130f * scaleX, 30f * scaleY),
                            strokeWidth = 6f * scaleX,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentShakes.toString(),
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "/ $targetShakes",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Shake indicator
            if (shakeTriggered) {
                PulseAnimation {
                    Text(
                        text = "SHAKE DETECTED!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Sage,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Shake harder to register",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Progress bar with gradient
            LinearProgressIndicator(
                progress = { currentShakes.toFloat() / targetShakes.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = Terracotta,
                trackColor = WarmBrown,
                drawStopIndicator = {}
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Motivational text — no emojis
            Text(
                text = when {
                    currentShakes == 0 -> "Let's get moving!"
                    currentShakes < targetShakes / 2 -> "You're doing great!"
                    currentShakes < targetShakes -> "Almost there!"
                    else -> "Amazing work!"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Amber,
                textAlign = TextAlign.Center
            )
        }
    }
}
