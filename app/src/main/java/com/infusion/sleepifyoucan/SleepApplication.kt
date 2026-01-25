package com.infusion.sleepifyoucan

import android.app.Application
import com.infusion.sleepifyoucan.data.AppDatabase

class SleepApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
