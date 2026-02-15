package com.infusion.sleepifyoucan.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.sqrt

class SleepTrackingRepository(
    private val sleepSessionDao: SleepSessionDao,
    private val sleepEventDao: SleepEventDao,
    private val sleepReportDao: SleepReportDao,
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var currentSessionId: Int? = null
    private var isTracking = false

    // Movement tracking
    private var lastAcceleration = 0f
    private var movementCount = 0
    private var snoringCount = 0

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && currentSessionId != null) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val magnitude = sqrt(x * x + y * y + z * z)
                val linearAcceleration = abs(magnitude - SensorManager.GRAVITY_EARTH)

                // Detect significant movement
                if (linearAcceleration > 2.0f && abs(linearAcceleration - lastAcceleration) > 1.0f) {
                    movementCount++
                    scope.launch {
                        recordSleepEvent(
                            sessionId = currentSessionId!!,
                            type = SleepEventType.MOVEMENT_DETECTED,
                            value = linearAcceleration
                        )
                    }
                }

                lastAcceleration = linearAcceleration
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    /**
     * Start sleep tracking session
     */
    suspend fun startSleepTracking(): Int {
        val session = SleepSession(
            startTime = System.currentTimeMillis(),
            quality = SleepQuality.UNKNOWN
        )

        val sessionId = sleepSessionDao.insertSleepSession(session).toInt()
        currentSessionId = sessionId

        // Record bedtime start event
        recordSleepEvent(sessionId, SleepEventType.BEDTIME_START)

        // Start sensor monitoring
        startSensorMonitoring()

        isTracking = true
        return sessionId
    }

    /**
     * Stop sleep tracking and generate report
     */
    suspend fun stopSleepTracking(alarmId: Int? = null): SleepReport? {
        val sessionId = currentSessionId ?: return null

        val endTime = System.currentTimeMillis()
        val session = sleepSessionDao.getSleepSessionById(sessionId) ?: return null

        // Calculate sleep metrics
        val events = sleepEventDao.getSleepEventsForSession(sessionId)
        val duration = endTime - session.startTime

        // Estimate sleep stages (simplified algorithm)
        val movementEvents = events.count { it.type == SleepEventType.MOVEMENT_DETECTED }
        val snoringEvents = events.count { it.type == SleepEventType.SNORING_DETECTED }

        val restlessnessScore = (movementEvents.toFloat() / duration.toFloat() * 1000000).coerceIn(0f, 1f)

        // Estimate deep sleep (less movement = more deep sleep)
        val deepSleepRatio = (1f - restlessnessScore).coerceIn(0.1f, 0.4f)
        val deepSleepDuration = (duration * deepSleepRatio).toLong()
        val lightSleepDuration = (duration * (1f - deepSleepRatio) * 0.8f).toLong()
        val awakeDuration = (duration * (1f - deepSleepRatio) * 0.2f).toLong()

        val quality = when {
            restlessnessScore < 0.2f -> SleepQuality.EXCELLENT
            restlessnessScore < 0.4f -> SleepQuality.GOOD
            restlessnessScore < 0.6f -> SleepQuality.FAIR
            else -> SleepQuality.POOR
        }

        // Update session
        val updatedSession = session.copy(
            endTime = endTime,
            alarmId = alarmId,
            quality = quality,
            totalDuration = duration,
            deepSleepDuration = deepSleepDuration,
            lightSleepDuration = lightSleepDuration,
            awakeDuration = awakeDuration,
            snoringEvents = snoringEvents,
            restlessnessScore = restlessnessScore
        )

        sleepSessionDao.updateSleepSession(updatedSession)

        // Record wake up event
        recordSleepEvent(sessionId, SleepEventType.WAKE_UP)

        // Stop sensor monitoring
        stopSensorMonitoring()

        // Generate sleep report
        val report = generateSleepReport(updatedSession, events)
        sleepReportDao.insertSleepReport(report)

        currentSessionId = null
        isTracking = false

        return report
    }

    /**
     * Check if sleep tracking is currently active
     */
    fun isSleepTrackingActive(): Boolean = isTracking

    /**
     * Get current sleep session
     */
    suspend fun getCurrentSleepSession(): SleepSession? {
        return if (isTracking) {
            currentSessionId?.let { sleepSessionDao.getSleepSessionById(it) }
        } else {
            null
        }
    }

    /**
     * Get recent sleep reports
     */
    fun getRecentSleepReports(): Flow<List<SleepReport>> {
        return sleepReportDao.getRecentSleepReports()
    }

    /**
     * Get sleep report for specific date
     */
    suspend fun getSleepReportForDate(date: Long): SleepReport? {
        return sleepReportDao.getSleepReportForDate(date)
    }

    /**
     * Record a sleep event
     */
    private suspend fun recordSleepEvent(
        sessionId: Int,
        type: SleepEventType,
        value: Float = 0f
    ) {
        val event = SleepEvent(
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            type = type,
            value = value
        )
        sleepEventDao.insertSleepEvent(event)
    }

    /**
     * Generate a sleep report with summary and recommendations
     */
    private fun generateSleepReport(session: SleepSession, events: List<SleepEvent>): SleepReport {
        val durationHours = session.totalDuration / (1000.0 * 60.0 * 60.0)
        val deepSleepHours = session.deepSleepDuration / (1000.0 * 60.0 * 60.0)
        val lightSleepHours = session.lightSleepDuration / (1000.0 * 60.0 * 60.0)

        val score = calculateSleepScore(session)

        val summary = buildString {
            append("You slept for ${String.format("%.1f", durationHours)} hours. ")
            append("Deep sleep: ${String.format("%.1f", deepSleepHours)}h, ")
            append("Light sleep: ${String.format("%.1f", lightSleepHours)}h. ")
            append("Quality: ${session.quality.name.lowercase().replaceFirstChar { it.uppercase() }}")
        }

        val recommendations = generateRecommendations(session)

        return SleepReport(
            sessionId = session.id,
            date = session.startTime,
            summary = summary,
            recommendations = recommendations,
            score = score
        )
    }

    /**
     * Calculate sleep score (0-100)
     */
    private fun calculateSleepScore(session: SleepSession): Int {
        var score = 50 // Base score

        // Duration score (7-9 hours ideal)
        val durationHours = session.totalDuration / (1000.0 * 60.0 * 60.0)
        val durationScore = when {
            durationHours >= 7.0 && durationHours <= 9.0 -> 25
            durationHours >= 6.0 && durationHours <= 10.0 -> 15
            else -> 5
        }

        // Deep sleep score (20-25% of total sleep ideal)
        val deepSleepRatio = session.deepSleepDuration.toFloat() / session.totalDuration.toFloat()
        val deepSleepScore = when {
            deepSleepRatio >= 0.2f && deepSleepRatio <= 0.25f -> 20
            deepSleepRatio >= 0.15f && deepSleepRatio <= 0.3f -> 15
            else -> 5
        }

        // Restlessness score
        val restlessnessScore = (20 * (1f - session.restlessnessScore)).toInt()

        score += durationScore + deepSleepScore + restlessnessScore

        return score.coerceIn(0, 100)
    }

    /**
     * Generate personalized sleep recommendations
     */
    private fun generateRecommendations(session: SleepSession): String {
        val recommendations = mutableListOf<String>()

        val durationHours = session.totalDuration / (1000.0 * 60.0 * 60.0)
        if (durationHours < 7.0) {
            recommendations.add("Try to get more sleep - aim for 7-9 hours per night.")
        }

        val deepSleepRatio = session.deepSleepDuration.toFloat() / session.totalDuration.toFloat()
        if (deepSleepRatio < 0.15f) {
            recommendations.add("Consider improving your sleep environment for better deep sleep.")
        }

        if (session.restlessnessScore > 0.6f) {
            recommendations.add("Your sleep was restless. Try reducing caffeine and screen time before bed.")
        }

        if (session.snoringEvents > 10) {
            recommendations.add("Frequent snoring detected. Consider consulting a sleep specialist.")
        }

        return if (recommendations.isNotEmpty()) {
            recommendations.joinToString(" ")
        } else {
            "Great job! Your sleep looks healthy. Keep up the good habits!"
        }
    }

    /**
     * Start sensor monitoring for movement detection
     */
    private fun startSensorMonitoring() {
        sensorManager.registerListener(
            sensorListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    /**
     * Stop sensor monitoring
     */
    private fun stopSensorMonitoring() {
        sensorManager.unregisterListener(sensorListener)
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopSensorMonitoring()
        scope.launch {
            if (isTracking) {
                stopSleepTracking()
            }
        }
    }
}
