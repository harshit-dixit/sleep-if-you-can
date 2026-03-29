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
import com.infusion.sleepifyoucan.ui.theme.BlackMute
import com.infusion.sleepifyoucan.ui.theme.OrangeAccent
import kotlin.math.abs
import kotlin.math.sqrt

private const val SQUAT_TAG = "SquatMissionScreen"

@Composable
fun SquatMissionScreen(
    targetSquats: Int,
    currentSquats: Int,
    onSquatDetected: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val sensorManager = remember {
        try { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
        catch (e: Exception) { Log.e(SQUAT_TAG, "Failed to get SensorManager", e); null }
    }
    val accelerometer = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    var acceleration by remember { mutableFloatStateOf(0f) }
    var squatPhase by remember { mutableStateOf(SquatPhase.WAITING) }
    var sensorError by remember { mutableStateOf(false) }

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                try {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val magnitude = sqrt(x * x + y * y + z * z)
                    val linearAcceleration = abs(magnitude - SensorManager.GRAVITY_EARTH)
                    acceleration = linearAcceleration

                    // State machine:
                    // WAITING -> detect quick downward movement start
                    // GOING_DOWN -> waiting for deceleration (bottom of squat)
                    // BOTTOM -> waiting for upward push
                    // GOING_UP -> detect return to rest = squat complete
                    when (squatPhase) {
                        SquatPhase.WAITING -> {
                            if (linearAcceleration > 2.5f) {
                                squatPhase = SquatPhase.GOING_DOWN
                            }
                        }
                        SquatPhase.GOING_DOWN -> {
                            if (linearAcceleration < 0.8f) {
                                squatPhase = SquatPhase.BOTTOM
                            }
                        }
                        SquatPhase.BOTTOM -> {
                            if (linearAcceleration > 3.0f) {
                                squatPhase = SquatPhase.GOING_UP
                            }
                        }
                        SquatPhase.GOING_UP -> {
                            if (linearAcceleration < 1.2f) {
                                // Completed a squat!
                                onSquatDetected()
                                squatPhase = SquatPhase.WAITING
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(SQUAT_TAG, "Error in accelerometer listener", e)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    fun registerSensor() {
        if (sensorManager == null || accelerometer == null) { sensorError = true; return }
        try {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        } catch (e: Exception) {
            Log.e(SQUAT_TAG, "Failed to register accelerometer", e)
            sensorError = true
        }
    }

    fun unregisterSensor() {
        try { sensorManager?.unregisterListener(sensorListener) }
        catch (e: Exception) { Log.w(SQUAT_TAG, "Failed to unregister accelerometer", e) }
    }

    DisposableEffect(sensorManager, accelerometer) {
        registerSensor()
        onDispose { unregisterSensor() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> registerSensor()
                Lifecycle.Event.ON_PAUSE -> unregisterSensor()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = "Do Squats mission" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Hold your phone in your hand and perform squats",
            style = MaterialTheme.typography.bodyLarge,
            color = androidx.compose.ui.graphics.Color.Gray,
            textAlign = TextAlign.Center
        )

        if (sensorError) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF4A1010))
            ) {
                Text(
                    text = "⚠️ Accelerometer unavailable.\nPlease switch to a different alarm mission.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color.DarkGray)
                .semantics { contentDescription = "Squats done: $currentSquats of $targetSquats" },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$currentSquats",
                    style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = OrangeAccent)
                )
                Text(
                    text = "/ $targetSquats",
                    style = MaterialTheme.typography.headlineSmall,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        val phaseText = when (squatPhase) {
            SquatPhase.WAITING -> "⏸ Ready — start your squat"
            SquatPhase.GOING_DOWN -> "⬇ Going down…"
            SquatPhase.BOTTOM -> "⬆ Push up!"
            SquatPhase.GOING_UP -> "↑ Coming up…"
        }

        Text(
            text = phaseText,
            style = MaterialTheme.typography.headlineSmall,
            color = androidx.compose.ui.graphics.Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Accel: ${"%.2f".format(acceleration)}",
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        LinearProgressIndicator(
            progress = { (currentSquats.toFloat() / targetSquats.toFloat()).coerceIn(0f, 1f) },
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
