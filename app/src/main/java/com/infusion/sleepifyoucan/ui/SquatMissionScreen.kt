package com.infusion.sleepifyoucan.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.infusion.sleepifyoucan.ui.theme.BlackMute
import com.infusion.sleepifyoucan.ui.theme.OrangeAccent
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun SquatMissionScreen(
    targetSquats: Int,
    currentSquats: Int,
    onSquatDetected: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    
    var acceleration by remember { mutableFloatStateOf(0f) }
    var squatPhase by remember { mutableStateOf(SquatPhase.WAITING) }
    
    val sensorListener = remember {
        object : SensorEventListener {
            private var lastAcceleration = 0f
            private var minAcceleration = Float.MAX_VALUE
            private var maxAcceleration = Float.MIN_VALUE
            
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    
                    // Calculate magnitude of acceleration
                    val magnitude = sqrt(x * x + y * y + z * z)
                    
                    // Remove gravity (approximately 9.81 m/s²)
                    val linearAcceleration = abs(magnitude - SensorManager.GRAVITY_EARTH)
                    
                    acceleration = linearAcceleration
                    
                    // Simple squat detection algorithm
                    // Squats typically show a pattern: down (deceleration) -> up (acceleration)
                    when (squatPhase) {
                        SquatPhase.WAITING -> {
                            if (linearAcceleration > 2.0f) { // Significant upward acceleration
                                squatPhase = SquatPhase.GOING_DOWN
                                minAcceleration = linearAcceleration
                            }
                        }
                        SquatPhase.GOING_DOWN -> {
                            if (linearAcceleration < 1.0f) { // Slowing down (bottom of squat)
                                squatPhase = SquatPhase.BOTTOM
                                maxAcceleration = linearAcceleration
                            }
                            minAcceleration = minOf(minAcceleration, linearAcceleration)
                        }
                        SquatPhase.BOTTOM -> {
                            if (linearAcceleration > 2.5f) { // Accelerating up
                                squatPhase = SquatPhase.GOING_UP
                            }
                        }
                        SquatPhase.GOING_UP -> {
                            if (linearAcceleration < 1.5f) { // Slowing down at top
                                // Completed a squat!
                                onSquatDetected()
                                squatPhase = SquatPhase.WAITING
                                minAcceleration = Float.MAX_VALUE
                                maxAcceleration = Float.MIN_VALUE
                            }
                            maxAcceleration = maxOf(maxAcceleration, linearAcceleration)
                        }
                    }
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not used
            }
        }
    }
    
    // Register sensor listener when composable is active
    DisposableEffect(sensorManager, accelerometer, sensorListener) {
        sensorManager.registerListener(
            sensorListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
        
        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }
    
    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    sensorManager.registerListener(
                        sensorListener,
                        accelerometer,
                        SensorManager.SENSOR_DELAY_GAME
                    )
                }
                Lifecycle.Event.ON_PAUSE -> {
                    sensorManager.unregisterListener(sensorListener)
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
            text = "Do Squats!",
            style = MaterialTheme.typography.headlineLarge,
            color = OrangeAccent,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Hold your phone in your pocket and perform squats",
            style = MaterialTheme.typography.bodyLarge,
            color = androidx.compose.ui.graphics.Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Progress indicator
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$currentSquats",
                    style = TextStyle(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeAccent
                    )
                )
                Text(
                    text = "/ $targetSquats",
                    style = MaterialTheme.typography.headlineSmall,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Phase indicator
        val phaseText = when (squatPhase) {
            SquatPhase.WAITING -> "Ready to start"
            SquatPhase.GOING_DOWN -> "Going down..."
            SquatPhase.BOTTOM -> "Bottom position"
            SquatPhase.GOING_UP -> "Coming up..."
        }
        
        Text(
            text = phaseText,
            style = MaterialTheme.typography.headlineSmall,
            color = androidx.compose.ui.graphics.Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Acceleration indicator
        Text(
            text = "Acceleration: %.2f".format(acceleration),
            style = MaterialTheme.typography.bodyMedium,
            color = androidx.compose.ui.graphics.Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Progress bar
        LinearProgressIndicator(
            progress = { currentSquats.toFloat() / targetSquats.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = OrangeAccent,
            trackColor = androidx.compose.ui.graphics.Color.DarkGray
        )
    }
}

private enum class SquatPhase {
    WAITING,
    GOING_DOWN,
    BOTTOM,
    GOING_UP
}
