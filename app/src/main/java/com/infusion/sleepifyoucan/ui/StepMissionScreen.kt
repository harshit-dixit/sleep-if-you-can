package com.infusion.sleepifyoucan.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.infusion.sleepifyoucan.ui.theme.BlackMute
import com.infusion.sleepifyoucan.ui.theme.OrangeAccent
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun StepMissionScreen(
    targetSteps: Int,
    currentSteps: Int,
    onStepDetected: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val stepCounter = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    
    // Permission state for Activity Recognition (needed for Step Counter on Android 10+)
    var hasPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true // Not required before Android 10
            }
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    // Request permission on start
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!hasPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }
    
    var stepCount by remember { mutableIntStateOf(0) }
    var acceleration by remember { mutableFloatStateOf(0f) }
    var usingHardwareCounter by remember { mutableStateOf(true) }
    var lastStepTime by remember { mutableLongStateOf(0L) }
    
    // Try to use hardware step counter first, fallback to accelerometer
    val useHardwareSensor = stepCounter != null
    
    val stepSensorListener = remember {
        object : SensorEventListener {
            private var initialStepCount = -1f
            
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    val currentSteps = event.values[0]
                    
                    if (initialStepCount < 0) {
                        initialStepCount = currentSteps
                    } else {
                        val stepsTaken = (currentSteps - initialStepCount).toInt()
                        if (stepsTaken > stepCount) {
                            val newSteps = stepsTaken - stepCount
                            repeat(newSteps) {
                                onStepDetected()
                            }
                            stepCount = stepsTaken
                        }
                    }
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }
    
    val accelerometerListener = remember {
        object : SensorEventListener {
            private var lastAcceleration = 0f
            private var lastVelocity = 0f
            private var lastPosition = 0f
            private var stepThreshold = 12f
            
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    
                    // Calculate magnitude
                    val magnitude = sqrt(x * x + y * y + z * z)
                    
                    // Remove gravity
                    val linearAcceleration = abs(magnitude - SensorManager.GRAVITY_EARTH)
                    acceleration = linearAcceleration
                    
                    // Simple step detection using acceleration peaks
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastStepTime > 300) { // Minimum time between steps
                        if (linearAcceleration > stepThreshold && lastAcceleration <= stepThreshold) {
                            // Detected a step
                            onStepDetected()
                            lastStepTime = currentTime
                        }
                    }
                    
                    lastAcceleration = linearAcceleration
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }
    
    // Register appropriate sensor listener
    DisposableEffect(sensorManager, useHardwareSensor) {
        if (useHardwareSensor) {
            sensorManager.registerListener(
                stepSensorListener,
                stepCounter,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            usingHardwareCounter = true
        } else {
            sensorManager.registerListener(
                accelerometerListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
            usingHardwareCounter = false
        }
        
        onDispose {
            sensorManager.unregisterListener(stepSensorListener)
            sensorManager.unregisterListener(accelerometerListener)
        }
    }
    
    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (useHardwareSensor) {
                        sensorManager.registerListener(
                            stepSensorListener,
                            stepCounter,
                            SensorManager.SENSOR_DELAY_NORMAL
                        )
                    } else {
                        sensorManager.registerListener(
                            accelerometerListener,
                            accelerometer,
                            SensorManager.SENSOR_DELAY_GAME
                        )
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    sensorManager.unregisterListener(stepSensorListener)
                    sensorManager.unregisterListener(accelerometerListener)
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
            text = "Take Steps!",
            style = MaterialTheme.typography.headlineLarge,
            color = OrangeAccent,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Walk around to register steps",
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
                    text = "$currentSteps",
                    style = TextStyle(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeAccent
                    )
                )
                Text(
                    text = "/ $targetSteps",
                    style = MaterialTheme.typography.headlineSmall,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Sensor type indicator
        Text(
            text = if (usingHardwareCounter) "Using hardware step counter" else "Using accelerometer detection",
            style = MaterialTheme.typography.bodyMedium,
            color = androidx.compose.ui.graphics.Color.Gray,
            textAlign = TextAlign.Center
        )
        
        if (!usingHardwareCounter) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Acceleration: %.2f".format(acceleration),
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Progress bar
        LinearProgressIndicator(
            progress = { currentSteps.toFloat() / targetSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = OrangeAccent,
            trackColor = androidx.compose.ui.graphics.Color.DarkGray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Instructions
        Text(
            text = "Keep your phone in your pocket and walk naturally",
            style = MaterialTheme.typography.bodyMedium,
            color = androidx.compose.ui.graphics.Color.LightGray,
            textAlign = TextAlign.Center
        )
    }
}
