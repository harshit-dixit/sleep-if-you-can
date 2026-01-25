package com.infusion.sleepifyoucan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.infusion.sleepifyoucan.data.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed. Rescheduling alarms...")
            
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            
            scope.launch {
                try {
                    val app = context.applicationContext as SleepApplication
                    val dao = app.database.alarmDao()
                    val scheduler = AlarmScheduler(context)
                    
                    val enabledAlarms = dao.getEnabledAlarms()
                    for (alarm in enabledAlarms) {
                        Log.d("BootReceiver", "Rescheduling alarm ${alarm.id}")
                        scheduler.schedule(alarm)
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
