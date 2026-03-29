package com.infusion.sleepifyoucan.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    // Config
    // 2.7g threshold: deliberate enough to avoid pocket/table false positives,
    // low enough to be easily triggered by hand shaking.
    private val SHAKE_THRESHOLD_GRAVITY = 2.7F
    private val SHAKE_SLOP_TIME_MS = 400 // Minimum ms between detected shakes
    private var shakeTimestamp: Long = 0
    
    private var onShakeListener: (() -> Unit)? = null

    fun start(listener: () -> Unit) {
        onShakeListener = listener
        accelerometer?.let {
            // SENSOR_DELAY_GAME gives ~50Hz sampling — good balance of accuracy and battery
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        if (accelerometer != null) {
            sensorManager.unregisterListener(this)
        }
        onShakeListener = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // gForce will be close to 1 when there is no movement.
        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            val now = System.currentTimeMillis()
            // Ignore shake events too close to each other (500ms)
            if (shakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return
            }
            shakeTimestamp = now
            onShakeListener?.invoke()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
