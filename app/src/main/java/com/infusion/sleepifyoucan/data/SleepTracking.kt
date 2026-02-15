package com.infusion.sleepifyoucan.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long, // Timestamp when sleep tracking started
    val endTime: Long? = null, // Timestamp when sleep tracking ended (alarm time)
    val alarmId: Int? = null, // Which alarm woke them up
    val quality: SleepQuality = SleepQuality.UNKNOWN,
    val totalDuration: Long = 0, // Total sleep duration in milliseconds
    val deepSleepDuration: Long = 0, // Estimated deep sleep in milliseconds
    val lightSleepDuration: Long = 0, // Estimated light sleep in milliseconds
    val awakeDuration: Long = 0, // Time awake during sleep period
    val snoringEvents: Int = 0, // Number of snoring detections
    val restlessnessScore: Float = 0f // 0-1 scale of how restless sleep was
)

@Entity(tableName = "sleep_events")
data class SleepEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val timestamp: Long,
    val type: SleepEventType,
    val value: Float = 0f // Additional data (e.g., movement intensity, sound level)
)

enum class SleepQuality {
    EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
}

enum class SleepEventType {
    BEDTIME_START, // User indicated they went to bed
    LIGHT_SLEEP_START,
    DEEP_SLEEP_START,
    AWAKE_PERIOD,
    SNORING_DETECTED,
    MOVEMENT_DETECTED,
    WAKE_UP // Final wake up event
}

@Entity(tableName = "sleep_reports")
data class SleepReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val date: Long, // Date of the report (start of sleep session)
    val summary: String, // Generated summary text
    val recommendations: String, // Sleep improvement tips
    val score: Int // Overall sleep score 0-100
)
