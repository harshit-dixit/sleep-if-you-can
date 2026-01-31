package com.infusion.sleepifyoucan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.infusion.sleepifyoucan.AlarmActivity
import com.infusion.sleepifyoucan.R
import kotlinx.coroutines.*

class RingtoneService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private var volumeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "ALARM_CHANNEL"
        const val ACTION_STOP = "STOP_ALARM"
        const val ACTION_START = "START_ALARM"
        
        // Escape tracking for penalty system
        private var escapeCount = 0
        private var currentAlarmId: Int = 0
        
        /**
         * Record an escape attempt when user leaves the alarm activity.
         */
        fun recordEscape(alarmId: Int) {
            if (alarmId == currentAlarmId) {
                escapeCount++
            }
        }
        
        /**
         * Get the penalty multiplier based on escape attempts.
         * Returns additional challenges to add to the mission.
         */
        fun getEscapePenalty(): Int {
            return when (escapeCount) {
                0 -> 0
                1 -> 2  // +2 extra problems or shakes
                2 -> 5  // +5 extra
                else -> 10 // Maximum penalty
            }
        }
        
        /**
         * Reset escape tracking for a new alarm.
         */
        fun resetForNewAlarm(alarmId: Int) {
            currentAlarmId = alarmId
            escapeCount = 0
        }
        
        /**
         * Check if there was an escape attempt.
         */
        fun hasEscapeAttempt(): Boolean = escapeCount > 0
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        // Acquire WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SleepIfYouCan:RingtoneServiceWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L /*10 minutes*/)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()

        val alarmId = intent?.getIntExtra("ALARM_ID", 0) ?: 0
        val ringtoneUriString = intent?.getStringExtra("RINGTONE_URI")
        val isVibrate = intent?.getBooleanExtra("IS_VIBRATE", true) ?: true

        // Prepare Activity Intent
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            // Forward extras to Activity
            intent?.extras?.let { putExtras(it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            alarmId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ALARM RINGING!")
            .setContentText("Shake to dismiss!")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        // Use Alarm ID for notification to handle concurrent alarms (rare but possible)
        startForeground(if (alarmId != 0) alarmId else 1, notification)

        startAlarm(ringtoneUriString, isVibrate)
        startVolumeEnforcement()
        
        // --- CRITICAL FIX: The Background Start Trap ---
        if (Settings.canDrawOverlays(this)) {
            // Plan A: Force the Activity open (Alarmy Style)
            // We can do this because we have the overlay permission!
            try {
                startActivity(fullScreenIntent)
            } catch (e: Exception) {
                // Fallback just in case
                e.printStackTrace()
            }
        } else {
            // Plan B: Android Standard (Heads-up Notification)
            // We ALREADY set setFullScreenIntent on the notification above.
            // That will show the Heads-up notification which users can tap.
            // If the screen is off, it *might* still show the activity depending on OS/Device,
            // but we can't force startExecutor without the permission.
        }

        return START_STICKY
    }

    private fun startAlarm(ringtoneUriString: String?, isVibrate: Boolean) {
        try {
            // Audio Focus
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val focusRequest = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            
            // Just request, we force volume anyway
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 // Simplified focus request for brevity, main logic is volume loop
             }

            val alarmUri: Uri = if (ringtoneUriString != null) {
                Uri.parse(ringtoneUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) 
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@RingtoneService, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            if (isVibrate) {
                val vibrationPattern = longArrayOf(0, 500, 500) // Wait 0, Vibrate 500, Sleep 500
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(vibrationPattern, 0)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun startVolumeEnforcement() {
        volumeJob = scope.launch {
            while (isActive) {
                try {
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
    
                    if (currentVolume != maxVolume) {
                        // The user tried to lower it! Force it back up.
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_ALARM,
                            maxVolume,
                            0 // Flags: 0 avoids showing the system UI slider repeatedly
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(1000) // Check every second
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        volumeJob?.cancel()
        scope.cancel() // Cancel all coroutines
        
        mediaPlayer?.stop()
        mediaPlayer?.release()
        vibrator?.cancel()
        
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null) // Sound is handled by MediaPlayer
                enableVibration(false) // Vibration handled by Vibrator
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
