package com.infusion.sleepifyoucan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val label: String? = null,
    val daysOfWeek: List<Int> = emptyList(), // 1=Sunday, 2=Monday, ...
    val ringtoneUri: String? = null, // Null = Default
    val isVibrate: Boolean = true,
    val isSnoozeEnabled: Boolean = true,
    val snoozeDuration: Int = 5,
    val missionConfig: MissionConfig = MissionConfig.Shake() // Default to Shake
)

sealed class MissionConfig {
    data class Shake(val targetShakes: Int = 20) : MissionConfig()
    data class Math(val difficulty: Difficulty = Difficulty.EASY, val problemCount: Int = 3) : MissionConfig()
    // Add more missions here (e.g. Photo, Barcode) logic later
}

enum class Difficulty {
    EASY, MEDIUM, HARD
}
