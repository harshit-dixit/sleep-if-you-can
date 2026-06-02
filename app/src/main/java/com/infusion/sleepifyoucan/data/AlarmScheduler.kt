package com.infusion.sleepifyoucan.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.infusion.sleepifyoucan.AlarmReceiver

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val gson = Gson()

    fun schedule(alarm: Alarm) {
        // 1. Calculate next trigger time
        val triggerTime = AlarmScheduleCalculator.nextTriggerTimeMillis(alarm)
        
        // 2. Schedule it
        scheduleExact(alarm, triggerTime, isSnooze = false)
    }

    fun scheduleSnooze(alarm: Alarm, durationMillis: Long) {
        val triggerTime = System.currentTimeMillis() + durationMillis
        scheduleExact(alarm, triggerTime, isSnooze = true)
    }

    private fun scheduleExact(alarm: Alarm, triggerTime: Long, isSnooze: Boolean) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            // PACK DATA: Serialize critical info into extras
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_ID", alarm.id)
            putExtra("MISSION_CONFIG_JSON", Converters().fromMissionConfig(alarm.missionConfig))
            // Note: We use the helper directly. 
            // Converters.fromMissionConfig returns a JSON string. Perfect.
            
            putExtra("RINGTONE_URI", alarm.ringtoneUri)
            putExtra("LABEL", alarm.label)
            putExtra("ALARM_SOUND", alarm.alarmSound.name)
            putExtra("IS_SNOOZE", isSnooze)
            putExtra("IS_VIBRATE", alarm.isVibrate)
            putExtra("IS_SNOOZE_ENABLED", alarm.isSnoozeEnabled)
            putExtra("SNOOZE_DURATION", alarm.snoozeDuration)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id, // Use unique ID to prevent collisions
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d("AlarmScheduler", "Scheduling alarm ${alarm.id} for: $triggerTime (Snooze: $isSnooze)")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                 alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                Log.e("AlarmScheduler", "Permission for exact alarms missing.")
                // Ideally show UI to user, but here we just fail/log
            }
        } else {
             alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancel(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Cancelled alarm ${alarm.id}")
    }
    
}
