package com.infusion.sleepifyoucan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.infusion.sleepifyoucan.ui.theme.BlackMute
import com.infusion.sleepifyoucan.ui.theme.OrangeAccent
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
            .background(BlackMute)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Wake Up Check",
            style = MaterialTheme.typography.headlineLarge,
            color = OrangeAccent,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Are you really awake?",
            style = MaterialTheme.typography.headlineMedium,
            color = androidx.compose.ui.graphics.Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Prove it by staying awake for a few seconds",
            style = MaterialTheme.typography.bodyLarge,
            color = androidx.compose.ui.graphics.Color.Gray,
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
                        androidx.compose.ui.graphics.Color.DarkGray 
                    else 
                        androidx.compose.ui.graphics.Color.Green.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (timeLeft > 0) timeLeft.toString() else "✓",
                style = TextStyle(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (timeLeft > 0) OrangeAccent else androidx.compose.ui.graphics.Color.Green
                )
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (timeLeft > 0) {
            Text(
                text = "Stay awake and focused...",
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color.LightGray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Manual confirm button (optional)
            OutlinedButton(
                onClick = onWakeUpConfirmed,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OrangeAccent
                )
            ) {
                Text("I'm Awake!")
            }
        } else {
            Text(
                text = "Confirmed! Turning off alarm...",
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color.Green,
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
            color = if (timeLeft > 0) OrangeAccent else androidx.compose.ui.graphics.Color.Green,
            trackColor = androidx.compose.ui.graphics.Color.DarkGray
        )
    }
}
