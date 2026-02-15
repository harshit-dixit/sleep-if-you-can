package com.infusion.sleepifyoucan.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun ShakeMissionScreen(
    currentShakes: Int,
    targetShakes: Int,
    shakeDetector: ShakeDetector,
    onShake: () -> Unit
) {
    var shakeTriggered by remember { mutableStateOf(false) }

    // Detect shakes and trigger animation
    LaunchedEffect(shakeDetector.shakeCount) {
        if (shakeDetector.shakeCount > 0) {
            shakeTriggered = true
            onShake()
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
                    color = Coral,
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

            // Progress indicator with bounce animation
            BounceAnimation(isPressed = shakeTriggered) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(
                            brush = GradientAccent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentShakes.toString(),
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "/ $targetShakes",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
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
                        color = Mint,
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
                color = Coral,
                trackColor = OceanBlue,
                drawStopIndicator = {}
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Motivational text
            Text(
                text = when {
                    currentShakes == 0 -> "Let's get moving! 🎯"
                    currentShakes < targetShakes / 2 -> "You're doing great! 💪"
                    currentShakes < targetShakes -> "Almost there! 🔥"
                    else -> "Amazing work! ⭐"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Gold,
                textAlign = TextAlign.Center
            )
        }
    }
}
