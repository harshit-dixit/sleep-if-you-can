package com.infusion.sleepifyoucan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

@Composable
fun WakeUpCheckScreen(
    onWakeUpConfirmed: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(5) }
    var isCountingDown by remember { mutableStateOf(false) }
    
    // Start countdown when screen appears
    LaunchedEffect(Unit) {
        isCountingDown = true
        for (i in 5 downTo 0) {
            timeLeft = i
            if (i > 0) {
                delay(1000)
            } else {
                // Auto-confirm after countdown
                onWakeUpConfirmed()
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Charcoal)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Wake Up Check",
            style = MaterialTheme.typography.headlineLarge,
            color = Terracotta,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Are you really awake?",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Prove it by staying awake for a few seconds",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Countdown timer
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(75.dp))
                .background(
                    if (timeLeft > 0) 
                        WarmBrown 
                    else 
                        Sage.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (timeLeft > 0) {
                Text(
                    text = timeLeft.toString(),
                    style = TextStyle(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Terracotta
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = Sage,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (timeLeft > 0) {
            Text(
                text = "Stay awake and focused...",
                style = MaterialTheme.typography.bodyLarge,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Manual confirm button (optional)
            OutlinedButton(
                onClick = onWakeUpConfirmed,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Terracotta
                )
            ) {
                Text("I'm Awake!")
            }
        } else {
            Text(
                text = "Confirmed! Turning off alarm...",
                style = MaterialTheme.typography.bodyLarge,
                color = Sage,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Progress indicator
        LinearProgressIndicator(
            progress = { (5 - timeLeft).toFloat() / 5f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (timeLeft > 0) Terracotta else Sage,
            trackColor = WarmBrown
        )
    }
}
