package com.infusion.sleepifyoucan

import android.app.Application
import com.infusion.sleepifyoucan.data.AppDatabase
import com.infusion.sleepifyoucan.data.SleepTrackingRepository

class SleepApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val sleepTrackingRepository: SleepTrackingRepository by lazy {
        SleepTrackingRepository(
            database.sleepSessionDao(),
            database.sleepEventDao(),
            database.sleepReportDao(),
            this
        )
    }
}
