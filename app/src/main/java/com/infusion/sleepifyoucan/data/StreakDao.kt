package com.infusion.sleepifyoucan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for streak-related database operations.
 */
@Dao
interface StreakDao {
    
    /**
     * Insert a new streak record. If one already exists for the day, replace it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: Streak)
    
    /**
     * Get all streaks in descending order by date.
     */
    @Query("SELECT * FROM streaks ORDER BY date DESC")
    fun getAllStreaks(): Flow<List<Streak>>
    
    /**
     * Get streaks for a specific date range (for weekly calendar display).
     * @param startDay Start epoch day (inclusive)
     * @param endDay End epoch day (inclusive)
     */
    @Query("SELECT * FROM streaks WHERE date >= :startDay AND date <= :endDay ORDER BY date ASC")
    fun getStreaksForRange(startDay: Long, endDay: Long): Flow<List<Streak>>
    
    /**
     * Get the most recent streak record.
     */
    @Query("SELECT * FROM streaks ORDER BY date DESC LIMIT 1")
    suspend fun getLatestStreak(): Streak?
    
    /**
     * Get streak count - consecutive successful days ending at today or yesterday.
     * This is calculated in the repository using the streak records.
     */
    @Query("SELECT * FROM streaks WHERE dismissedSuccessfully = 1 ORDER BY date DESC")
    suspend fun getSuccessfulStreaks(): List<Streak>
    
    /**
     * Check if a streak exists for a specific day.
     */
    @Query("SELECT * FROM streaks WHERE date = :epochDay LIMIT 1")
    suspend fun getStreakForDay(epochDay: Long): Streak?
    
    /**
     * Get total count of successful wake-ups.
     */
    @Query("SELECT COUNT(*) FROM streaks WHERE dismissedSuccessfully = 1")
    fun getTotalSuccessfulWakeUps(): Flow<Int>
    
    /**
     * Delete all streak records (for testing/reset).
     */
    @Query("DELETE FROM streaks")
    suspend fun deleteAll()
}
