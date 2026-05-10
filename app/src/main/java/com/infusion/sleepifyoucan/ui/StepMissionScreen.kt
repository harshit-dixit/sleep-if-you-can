package com.infusion.sleepifyoucan.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.infusion.sleepifyoucan.ui.theme.*
import kotlin.math.abs
import kotlin.math.sqrt

private const val TAG = "StepMissionScreen"

// Windowed average window size for accelerometer fallback
private const val WINDOW_SIZE = 5
// Step detection threshold for windowed average (tuned for normal walking)
private const val STEP_THRESHOLD = 9.5f
// Minimum time between steps (ms) to avoid double-counting
private const val MIN_STEP_INTERVAL_MS = 350L

@Composable
fun StepMissionScreen(
    targetSteps: Int,
    currentSteps: Int,
    onStepDetected: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check and request ACTIVITY_RECOGNITION permission (Android 10+)
    var hasPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) Log.w(TAG, "ACTIVITY_RECOGNITION permission denied; falling back to accelerometer")
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    val sensorManager = remember {
        try { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
        catch (e: Exception) { Log.e(TAG, "Failed to get SensorManager", e); null }
    }

    val stepCounter = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    val accelerometer = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    val useHardwareSensor = stepCounter != null && hasPermission

    var usingHardwareCounter by remember { mutableStateOf(useHardwareSensor) }
    var sensorError by remember { mutableStateOf(false) }

    var debugAcceleration by remember { mutableFloatStateOf(0f) }
    var debugWindowAvg by remember { mutableFloatStateOf(0f) }

    val stepSensorListener = remember {
        object : SensorEventListener {
            private var initialStepCount = -1f
            private var lastReportedSteps = 0

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
                try {
                    val totalSteps = event.values[0]
                    if (initialStepCount < 0f) {
                        initialStepCount = totalSteps
                    }
                    val stepsTaken = (totalSteps - initialStepCount).toInt().coerceAtLeast(0)
                    val newSteps = stepsTaken - lastReportedSteps
                    if (newSteps > 0) {
                        repeat(newSteps) { onStepDetected() }
                        lastReportedSteps = stepsTaken
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in step counter listener", e)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    val accelStepListener = remember {
        object : SensorEventListener {
            private val window = ArrayDeque<Float>(WINDOW_SIZE)
            private var lastWindowAvg = 0f
            private var lastStepTime = 0L
            private var wasAboveThreshold = false

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                try {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val linearAccel = abs(sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH)

                    debugAcceleration = linearAccel

                    if (window.size >= WINDOW_SIZE) window.removeFirst()
                    window.addLast(linearAccel)
                    val avg = window.average().toFloat()
                    debugWindowAvg = avg

                    val now = System.currentTimeMillis()
                    if (avg > STEP_THRESHOLD && !wasAboveThreshold && now - lastStepTime > MIN_STEP_INTERVAL_MS) {
                        onStepDetected()
                        lastStepTime = now
                        wasAboveThreshold = true
                    } else if (avg <= STEP_THRESHOLD) {
                        wasAboveThreshold = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in accelerometer listener", e)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    fun registerSensors() {
        if (sensorManager == null) { sensorError = true; return }
        try {
            if (useHardwareSensor) {
                sensorManager.registerListener(stepSensorListener, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
                usingHardwareCounter = true
            } else if (accelerometer != null) {
                sensorManager.registerListener(accelStepListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                usingHardwareCounter = false
            } else {
                sensorError = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register sensor listeners", e)
            sensorError = true
        }
    }

    fun unregisterSensors() {
        try {
            sensorManager?.unregisterListener(stepSensorListener)
            sensorManager?.unregisterListener(accelStepListener)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister sensor listeners", e)
        }
    }

    DisposableEffect(useHardwareSensor) {
        registerSensors()
        onDispose { unregisterSensors() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> registerSensors()
                Lifecycle.Event.ON_PAUSE -> unregisterSensors()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // --- UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Charcoal)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Take Steps!",
            style = MaterialTheme.typography.headlineLarge,
            color = Terracotta,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = "Take Steps mission" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Walk around to register steps",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        if (sensorError) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DustyRose.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = DustyRose)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Step sensor unavailable on this device.\nPlease switch to a different alarm mission.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Progress circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(WarmBrown)
                .semantics { contentDescription = "Steps taken: $currentSteps of $targetSteps" },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$currentSteps",
                    style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Terracotta)
                )
                Text(
                    text = "/ $targetSteps",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (usingHardwareCounter) Icons.Default.Sensors else Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (usingHardwareCounter) "Hardware step counter active" else "Accelerometer detection active",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }

        if (!usingHardwareCounter && !sensorError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Accel: ${"%.2f".format(debugAcceleration)} | Avg($WINDOW_SIZE): ${"%.2f".format(debugWindowAvg)} | Thresh: $STEP_THRESHOLD",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        LinearProgressIndicator(
            progress = { (currentSteps.toFloat() / targetSteps.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = Terracotta,
            trackColor = WarmBrown
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Keep your phone in your pocket or hand and walk naturally",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
