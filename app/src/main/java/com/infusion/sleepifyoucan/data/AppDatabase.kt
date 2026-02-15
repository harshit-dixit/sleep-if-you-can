package com.infusion.sleepifyoucan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Alarm::class, Streak::class, SleepSession::class, SleepEvent::class, SleepReport::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun streakDao(): StreakDao
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun sleepEventDao(): SleepEventDao
    abstract fun sleepReportDao(): SleepReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sleep_if_you_can_db"
                )
                .fallbackToDestructiveMigration() // Reset DB for schema changes
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

