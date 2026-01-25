package com.infusion.sleepifyoucan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infusion.sleepifyoucan.service.RingtoneService
import com.infusion.sleepifyoucan.utils.ShakeDetector
import com.infusion.sleepifyoucan.utils.turnScreenOnAndKeyguardOff
import com.infusion.sleepifyoucan.ui.theme.SleepIfYouCanTheme

class AlarmActivity : ComponentActivity() {

    private lateinit var shakeDetector: ShakeDetector
    private var shakeCount by mutableIntStateOf(0)
    private val TARGET_SHAKES = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguardOff()

        // Disable Back Button
        onBackPressedDispatcher.addCallback(this) {
            // Do nothing, forcing user to complete mission
        }

        shakeDetector = ShakeDetector(this)

        setContent {
            SleepIfYouCanTheme { // Ensure Theme is available, or use defaults
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmScreen(
                        currentShakes = shakeCount,
                        targetShakes = TARGET_SHAKES
                    )
                }
            }
        }
        
        startMission()
    }

    private fun startMission() {
        shakeDetector.start {
            shakeCount++
            if (shakeCount >= TARGET_SHAKES) {
                onMissionComplete()
            }
        }
    }
    
    private fun onMissionComplete() {
        shakeDetector.stop()
        
        // Stop the Service
        val stopIntent = Intent(this, RingtoneService::class.java).apply {
            action = RingtoneService.ACTION_STOP
        }
        startService(stopIntent)
        
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        shakeDetector.stop()
    }
}

@Composable
fun AlarmScreen(currentShakes: Int, targetShakes: Int) {
    val progress by animateFloatAsState(
        targetValue = currentShakes.toFloat() / targetShakes.toFloat(),
        label = "Progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ALARM RINGING!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Shake your phone to dismiss!",
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 12.dp,
            )
            Text(
                text = "${currentShakes} / ${targetShakes}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "KEEP SHAKING!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
