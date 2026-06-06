package com.infusion.sleepifyoucan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.infusion.sleepifyoucan.service.RingtoneService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // NO DB ACCESS HERE to avoid ANR/Crash
        
        val serviceIntent = Intent(context, RingtoneService::class.java).apply {
            action = "START_ALARM"
             // Forward all extras (Mission, Ringtone, ID, etc.)
            putExtras(intent)
        }
        
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
