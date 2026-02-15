package com.infusion.sleepifyoucan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {
    @Insert
    suspend fun insertSleepSession(session: SleepSession): Long

    @Update
    suspend fun updateSleepSession(session: SleepSession)

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    suspend fun getSleepSessionById(id: Int): SleepSession?

    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSleepSession(): SleepSession?

    @Query("SELECT * FROM sleep_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSleepSession(): SleepSession?

    @Query("SELECT * FROM sleep_sessions WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun getCompletedSleepSessions(): Flow<List<SleepSession>>

    @Query("SELECT * FROM sleep_sessions WHERE date(startTime / 1000, 'unixepoch') = date(:date / 1000, 'unixepoch')")
    suspend fun getSleepSessionsForDate(date: Long): List<SleepSession>

    @Query("SELECT COUNT(*) FROM sleep_sessions WHERE endTime IS NOT NULL AND startTime >= :startTime")
    suspend fun getSleepSessionCountSince(startTime: Long): Int
}

@Dao
interface SleepEventDao {
    @Insert
    suspend fun insertSleepEvent(event: SleepEvent): Long

    @Query("SELECT * FROM sleep_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSleepEventsForSession(sessionId: Int): List<SleepEvent>

    @Query("SELECT COUNT(*) FROM sleep_events WHERE sessionId = :sessionId AND type = :eventType")
    suspend fun getEventCountForSession(sessionId: Int, eventType: SleepEventType): Int

    @Query("SELECT * FROM sleep_events WHERE type = :eventType AND timestamp >= :startTime ORDER BY timestamp DESC")
    suspend fun getRecentEvents(eventType: SleepEventType, startTime: Long): List<SleepEvent>
}

@Dao
interface SleepReportDao {
    @Insert
    suspend fun insertSleepReport(report: SleepReport): Long

    @Query("SELECT * FROM sleep_reports WHERE sessionId = :sessionId")
    suspend fun getSleepReportForSession(sessionId: Int): SleepReport?

    @Query("SELECT * FROM sleep_reports ORDER BY date DESC LIMIT 10")
    fun getRecentSleepReports(): Flow<List<SleepReport>>

    @Query("SELECT * FROM sleep_reports WHERE date = :date")
    suspend fun getSleepReportForDate(date: Long): SleepReport?
}
