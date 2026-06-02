package com.infusion.sleepifyoucan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.infusion.sleepifyoucan.AlarmActivity
import com.infusion.sleepifyoucan.R
import com.infusion.sleepifyoucan.data.AlarmSound
import com.infusion.sleepifyoucan.data.AppPreferences
import com.infusion.sleepifyoucan.data.MissionAudioBehavior
import com.infusion.sleepifyoucan.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.*
import android.app.Service

class RingtoneService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private var volumeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var appPreferences = AppPreferences()
    private var isVibrate: Boolean = true
    private var isVibrating: Boolean = false

    companion object {
        const val CHANNEL_ID = "ALARM_CHANNEL"
        const val ACTION_STOP = "STOP_ALARM"
        const val ACTION_START = "START_ALARM"
        
        // Escape tracking for penalty system
        private var escapeCount = 0
        private var currentAlarmId: Int = 0
        
        @Volatile
        var lastInteractionTime: Long = 0L
        
        fun recordUserInteraction() {
            lastInteractionTime = System.currentTimeMillis()
        }
        
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
        resetForNewAlarm(alarmId)
        lastInteractionTime = 0L // reset interaction time

        // Load user preferences
        scope.launch {
            try {
                appPreferences = UserPreferencesRepository(applicationContext).preferences.first()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val ringtoneUriString = intent?.getStringExtra("RINGTONE_URI")
        val alarmSoundString = intent?.getStringExtra("ALARM_SOUND")
        val alarmSound = try {
            AlarmSound.valueOf(alarmSoundString ?: "DEFAULT")
        } catch (e: Exception) {
            AlarmSound.DEFAULT
        }
        isVibrate = intent?.getBooleanExtra("IS_VIBRATE", true) ?: true

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

        startAlarm(ringtoneUriString, alarmSound)
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

    private fun startAlarm(ringtoneUriString: String?, alarmSound: AlarmSound) {
        try {
            // Select alarm sound based on AlarmSound enum
            val alarmUri: Uri = when {
                ringtoneUriString != null -> Uri.parse(ringtoneUriString)
                else -> getAlarmSoundUri(alarmSound)
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
                // Start with lower volume for escalation
                setVolume(0.3f, 0.3f)
                start()
            }

            startVibration()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        if (isVibrate && !isVibrating) {
            val vibrationPattern = longArrayOf(0, 500, 500) // Wait 0, Vibrate 500, Sleep 500
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(vibrationPattern, 0)
            }
            isVibrating = true
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
        isVibrating = false
    }

    private fun getAlarmSoundUri(alarmSound: AlarmSound): Uri {
        return when (alarmSound) {
            AlarmSound.DEFAULT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
    }
    
    private fun startVolumeEnforcement() {
        volumeJob = scope.launch {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            
            // Volume escalation: gradually increase volume over 30 seconds
            val escalationDurationMs = 30000L // 30 seconds
            val escalationSteps = 30 // 30 steps
            val volumeIncrement = (maxVolume / escalationSteps).coerceAtLeast(1)
            val stepDurationMs = escalationDurationMs / escalationSteps
            
            val startTime = System.currentTimeMillis()
            
            while (isActive) {
                try {
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = currentTime - startTime
                    
                    // Determine baseline target volume
                    val isEscalating = appPreferences.volumeEscalation
                    val targetVolume = if (isEscalating) {
                        val currentStep = (elapsedTime / stepDurationMs).toInt().coerceIn(1, escalationSteps)
                        (volumeIncrement * currentStep).coerceIn(1, maxVolume)
                    } else {
                        maxVolume
                    }
                    
                    // Check user interaction for Mission Audio Behavior
                    val now = System.currentTimeMillis()
                    val interactedRecently = lastInteractionTime > 0L && (now - lastInteractionTime) < 5000L // 5 seconds
                    val pausedRecently = lastInteractionTime > 0L && (now - lastInteractionTime) < 10000L // 10 seconds
                    
                    var finalVolume = targetVolume
                    var shouldPause = false
                    
                    when (appPreferences.missionAudioBehavior) {
                        MissionAudioBehavior.ALWAYS_PLAY -> {
                            // Do nothing, play at standard target volume
                        }
                        MissionAudioBehavior.REDUCE_ON_ACTIVITY -> {
                            if (interactedRecently) {
                                finalVolume = (targetVolume * 0.5f).toInt().coerceAtLeast(1)
                            }
                        }
                        MissionAudioBehavior.PAUSE_ON_ACTIVITY -> {
                            if (pausedRecently) {
                                shouldPause = true
                            }
                        }
                    }
                    
                    // Apply volume / pause state
                    if (shouldPause) {
                        if (mediaPlayer?.isPlaying == true) {
                            mediaPlayer?.pause()
                        }
                        stopVibration()
                    } else {
                        if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                            mediaPlayer?.start()
                        }
                        startVibration()
                        
                        // Force system volume if user attempts to lower it (unless we are in reduced/paused mode)
                        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                        if (!interactedRecently && currentVolume < finalVolume) {
                            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, finalVolume, 0)
                        } else if (interactedRecently) {
                            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, finalVolume, 0)
                        }
                        
                        val mediaVolume = finalVolume.toFloat() / maxVolume.toFloat()
                        mediaPlayer?.setVolume(mediaVolume, mediaVolume)
                    }
                    
                    delay(500) // Check every 500ms
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(1000)
                }
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
