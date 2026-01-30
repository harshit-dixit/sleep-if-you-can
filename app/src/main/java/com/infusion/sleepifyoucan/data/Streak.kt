package com.infusion.sleepifyoucan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a daily streak record for when a user successfully wakes up
 * by completing an alarm mission.
 */
@Entity(tableName = "streaks")
data class Streak(
    @PrimaryKey
    val date: Long,  // Epoch day (days since Jan 1, 1970) - ensures one entry per day
    val alarmId: Int,
    val dismissedSuccessfully: Boolean,
    val dismissTime: Long,  // Timestamp when alarm was dismissed
    val missionType: String  // "SHAKE" or "MATH"
)

/**
 * Settings for streak behavior
 */
data class StreakSettings(
    val allowFreezeDays: Boolean = true,
    val maxFreezeDays: Int = 1  // Default allows 1 freeze day
)
