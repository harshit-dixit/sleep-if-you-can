package com.infusion.sleepifyoucan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.infusion.sleepifyoucan.ui.theme.*

@Composable
fun OnboardingFlow(
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GradientPrimary) // Warm charcoal gradient
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Content based on step
            when (currentStep) {
                0 -> {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        "Welcome to\nSleep If You Can",
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "The alarm clock that actually wakes you up.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                1 -> {
                    // Mission icons grid
                    LazyVerticalGridDemo()
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        "Mission-Based Alarms",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Dismiss your alarm by shaking your phone, solving math, typing a phrase, or scanning a saved code.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                2 -> {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        "Ready to start?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Never oversleep again.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page indicators
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentStep) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (index == currentStep) Terracotta else TextTertiary)
                        )
                    }
                }
                
                Button(
                    onClick = {
                        if (currentStep < 2) {
                            currentStep++
                        } else {
                            onComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Terracotta,
                        contentColor = TextOnAccent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(if (currentStep < 2) "Next" else "Get Started", fontWeight = FontWeight.Bold)
                    if (currentStep < 2) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LazyVerticalGridDemo() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        MissionChip(Icons.Default.Vibration, "Shake")
        MissionChip(Icons.Default.Calculate, "Math")
        MissionChip(Icons.Default.Keyboard, "Typing")
    }
}

@Composable
private fun MissionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    GlassCardAccent(
        modifier = Modifier.width(80.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Terracotta, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
        }
    }
}
