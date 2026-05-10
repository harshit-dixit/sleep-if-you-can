package com.infusion.sleepifyoucan.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit

/**
 * Repository for streak-related operations.
 * Handles streak calculation, milestone detection, and settings.
 */
class StreakRepository(
    private val streakDao: StreakDao,
    private val context: Context
) {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("streak_prefs", Context.MODE_PRIVATE)
    }
    
    // Settings
    var allowFreezeDays: Boolean
        get() = prefs.getBoolean("allow_freeze_days", true)
        set(value) = prefs.edit().putBoolean("allow_freeze_days", value).apply()
    
    var maxFreezeDays: Int
        get() = prefs.getInt("max_freeze_days", 1)
        set(value) = prefs.edit().putInt("max_freeze_days", value).apply()
    
    /**
     * Record a successful wake-up when user completes alarm mission.
     */
    suspend fun recordSuccessfulWakeUp(alarmId: Int, missionType: String) {
        val today = LocalDate.now().toEpochDay()
        val streak = Streak(
            date = today,
            alarmId = alarmId,
            dismissedSuccessfully = true,
            dismissTime = System.currentTimeMillis(),
            missionType = missionType
        )
        streakDao.insertStreak(streak)
    }
    
    /**
     * Calculate current streak count considering freeze days.
     * Returns the number of consecutive days the user has woken up successfully.
     */
    suspend fun getCurrentStreakCount(): Int {
        val successfulStreaks = streakDao.getSuccessfulStreaks()
        if (successfulStreaks.isEmpty()) return 0
        
        val today = LocalDate.now().toEpochDay()
        val allowedGap = if (allowFreezeDays) maxFreezeDays else 0
        
        var streakCount = 0
        var lastDate = today + 1  // Start from "tomorrow" so today counts
        
        for (streak in successfulStreaks) {
            val gap = lastDate - streak.date - 1
            
            if (gap <= allowedGap) {
                streakCount++
                lastDate = streak.date
            } else {
                break  // Streak broken
            }
        }
        
        // Check if the streak is still active (today or yesterday must be in streak)
        val latestStreakDate = successfulStreaks.firstOrNull()?.date ?: return 0
        val daysSinceLastStreak = today - latestStreakDate
        
        if (daysSinceLastStreak > allowedGap + 1) {
            return 0  // Streak expired
        }
        
        return streakCount
    }
    
    /**
     * Get weekly progress for calendar display (M T W T F S S).
     * Returns a map of epoch day to whether that day has a successful streak.
     */
    suspend fun getWeeklyProgress(): Map<Long, Boolean> {
        val today = LocalDate.now()
        val startOfWeek = today.with(DayOfWeek.MONDAY)
        val endOfWeek = today.with(DayOfWeek.SUNDAY)
        
        val startDay = startOfWeek.toEpochDay()
        val endDay = endOfWeek.toEpochDay()
        
        val result = mutableMapOf<Long, Boolean>()
        
        // Initialize all days as false
        for (day in startDay..endDay) {
            result[day] = false
        }
        
        // Mark successful days
        val streaks = streakDao.getSuccessfulStreaks()
        for (streak in streaks) {
            if (streak.date in startDay..endDay) {
                result[streak.date] = true
            }
        }
        
        return result
    }
    
    /**
     * Check if current streak is a milestone.
     * Milestones: 7 days (weekly), 30 days (monthly), multiples of 5
     */
    suspend fun getCurrentMilestone(): StreakMilestone? {
        val count = getCurrentStreakCount()
        
        return when {
            count == 7 -> StreakMilestone.WEEKLY
            count == 30 -> StreakMilestone.MONTHLY
            count > 0 && count % 5 == 0 -> StreakMilestone.MULTIPLE_OF_FIVE
            else -> null
        }
    }
    
    /**
     * Get a motivational message based on streak status.
     * Uses hardcoded quotes that rotate daily for active streaks.
     */
    suspend fun getMotivationalMessage(): String {
        val count = getCurrentStreakCount()
        val milestone = getCurrentMilestone()
        
        return when {
            milestone == StreakMilestone.MONTHLY -> "Amazing! One month of consistent wake-ups!"
            milestone == StreakMilestone.WEEKLY -> "One full week! You're on fire!"
            milestone == StreakMilestone.MULTIPLE_OF_FIVE -> "⭐ $count days! Keep the momentum going!"
            count >= 3 -> MotivationalQuotes.getQuoteOfTheDay()
            count >= 1 -> "Great start! Keep it going tomorrow!"
            else -> MotivationalQuotes.getQuoteOfTheDay()
        }
    }
    
    /**
     * Get flow of total successful wake-ups.
     */
    fun getTotalSuccessfulWakeUps(): Flow<Int> = streakDao.getTotalSuccessfulWakeUps()
    
    /**
     * Get all streaks as a flow.
     */
    fun getAllStreaks(): Flow<List<Streak>> = streakDao.getAllStreaks()
}

/**
 * Types of streak milestones to celebrate.
 */
enum class StreakMilestone {
    WEEKLY,          // 7 days
    MONTHLY,         // 30 days
    MULTIPLE_OF_FIVE // 5, 10, 15, 20...
}
