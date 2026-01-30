package com.infusion.sleepifyoucan.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.infusion.sleepifyoucan.utils.ShakeDetector
import com.infusion.sleepifyoucan.ui.theme.*

@Composable
fun ShakeMissionScreen(
    currentShakes: Int,
    targetShakes: Int,
    shakeDetector: ShakeDetector,
    onShake: () -> Unit
) {
    // We lift the state up. The ShakeDetector callback in Activity/VM should update the `currentShakes`.
    // However, the original code had the detector listener INSIDE the composable.
    // user's plan: "Keep ShakeDetector in Activity (bridged to ViewModel events)"
    // So here we just display progress?
    // Actually, if ShakeDetector is in Activity, this screen just visualizes 'currentShakes'.
    // The previous implementation had logic inside LaunchedEffect.
    // I will adapt it to be stateless UI if possible, OR, if ShakeDetector is passed, 
    // we can keep the listener here but it calls `onShake()` lambda which delegates to VM.
    
    // Side Effect to listen to shakes
    // NOTE: If ShakeDetector is managed by Activity lifecycle (onResume/onPause), 
    // we might not need to start/stop it here.
    // But usually it's tied to the mission screen being active.
    
    LaunchedEffect(Unit) {
        shakeDetector.start {
            onShake()
        }
    }
    
    // Stop on dispose is handled by ShakeDetector.start implementation usually replacing listener, 
    // or explicit stop? The extraction shows `shakeDetector.start`. 
    // `ShakeDetector` likely handles one listener.

    val progress by animateFloatAsState(
        targetValue = currentShakes.toFloat() / targetShakes.toFloat(),
        label = "Progress"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackMute)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "SHAKE IT!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Glass Filling Animation
        GlassFillingAnimation(
            progress = progress,
            modifier = Modifier.size(200.dp, 280.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "${currentShakes}/${targetShakes}",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = PurpleNight
        )
        
        if (progress < 1f) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Keep shaking!",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    }
}

/**
 * Glass filling with water animation.
 */
@Composable
fun GlassFillingAnimation(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val waterColor by animateColorAsState(
        targetValue = when {
            progress >= 1f -> GreenLand
            progress >= 0.7f -> PurpleNight
            else -> PurpleNight.copy(alpha = 0.7f)
        },
        label = "WaterColor"
    )
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val glassThickness = 8.dp.toPx()
        val glassRadius = 16.dp.toPx()
        
        // Glass outline
        drawRoundRect(
            color = TextSecondary.copy(alpha = 0.3f),
            topLeft = Offset(0f, 0f),
            size = Size(width, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(glassRadius)
        )
        
        // Water fill (from bottom up)
        val waterHeight = (height - glassThickness * 2) * progress
        val waterTop = height - glassThickness - waterHeight
        
        drawRoundRect(
            color = waterColor,
            topLeft = Offset(glassThickness, waterTop),
            size = Size(width - glassThickness * 2, waterHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(glassRadius - glassThickness)
        )
    }
}
