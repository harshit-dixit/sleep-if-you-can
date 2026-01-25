package com.infusion.sleepifyoucan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed. Checking for saved alarms...")
            
            val sharedPref = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
            val alarmTime = sharedPref.getLong("ALARM_TIME", -1L)
            
            if (alarmTime != -1L) {
                if (alarmTime > System.currentTimeMillis()) {
                    Log.d("BootReceiver", "Rescheduling alarm for: $alarmTime")
                    val scheduler = com.infusion.sleepifyoucan.data.AlarmScheduler(context)
                    scheduler.schedule(alarmTime)
                } else {
                    Log.d("BootReceiver", "Saved alarm is in the past. Ignoring.")
                    // Optional: clear it
                    sharedPref.edit().remove("ALARM_TIME").apply()
                }
            }
        }
    }
}
